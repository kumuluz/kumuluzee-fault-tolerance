/*
 *  Copyright (c) 2014-2017 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.kumuluz.ee.fault.tolerance;

import com.kumuluz.ee.fault.tolerance.commands.FallbackHelper;
import com.kumuluz.ee.fault.tolerance.commands.HystrixCommandConfiguration;
import com.kumuluz.ee.fault.tolerance.configurations.hystrix.CommandHystrixConfigurationUtil;
import com.kumuluz.ee.fault.tolerance.configurations.hystrix.HystrixFaultToleranceConfigurationManager;
import com.kumuluz.ee.fault.tolerance.configurations.hystrix.ThreadPoolHystrixConfigurationUtil;
import com.kumuluz.ee.fault.tolerance.configurations.retry.RetryConfig;
import com.kumuluz.ee.fault.tolerance.configurations.retry.RetryConfigurationManager;
import com.kumuluz.ee.fault.tolerance.enums.FaultToleranceType;
import com.kumuluz.ee.fault.tolerance.interfaces.FaultToleranceExecutor;
import com.kumuluz.ee.fault.tolerance.metrics.TimeoutMetricsCollection;
import com.kumuluz.ee.fault.tolerance.models.ConfigurationProperty;
import com.kumuluz.ee.fault.tolerance.models.ExecutionMetadata;
import com.netflix.hystrix.*;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.weld.context.RequestContext;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Hystrix implementation of fault tolerance executor
 *
 * @author Luka Šarc
 * @since 1.0.0
 */
@ApplicationScoped
public class HystrixFaultToleranceExecutorImpl implements FaultToleranceExecutor {

    private static final String NAME = "hystrix";

    private static final Logger log = Logger.getLogger(HystrixFaultToleranceExecutorImpl.class.getName());

    private static HashMap<String, HystrixCommandConfiguration> hystrixCommandConfigurations = new HashMap<>();
    private static HashMap<String, HystrixCommandKey> hystrixCommandKeys = new HashMap<>();
    private static HashMap<String, HystrixThreadPoolKey> hystrixThreadPoolKeys = new HashMap<>();

    @Inject
    private HystrixFaultToleranceConfigurationManager configManager;

    @Inject
    private RetryConfigurationManager retryManager;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Object execute(InvocationContext invocationContext, RequestContext requestContext,
                          ExecutionMetadata metadata) throws Exception {

        HystrixCommandConfiguration hystrixCommandConfig = getHystrixCommandSetter(metadata);

        metadata.getCommonMetricsCollection(invocationContext.getMethod().getName())
                .ifPresent(c -> c.getTotalInvocations().inc());

        try {
            if (metadata.getRetry() == null) {
                return executeWithHystrix(hystrixCommandConfig, invocationContext, requestContext, metadata);
            } else {
                return executeWithRetry(hystrixCommandConfig, invocationContext, requestContext, metadata,
                        null, 1, null);
            }
        } catch (Exception e) {
            metadata.getCommonMetricsCollection(invocationContext.getMethod().getName())
                    .ifPresent(c -> c.getFailedInvocations().inc());

            throw e;
        }
    }

    private Object executeWithRetry(HystrixCommandConfiguration hystrixCommand, InvocationContext invocationContext,
                                    RequestContext requestContext, ExecutionMetadata metadata,
                                    RetryConfig retryConfig, int execCnt, Instant executionStart) throws Exception {

        if (retryConfig == null)
            retryConfig = retryManager.getRetryConfig(metadata.getIdentifier());

        if (execCnt > 1) {
            log.info("Retry attempt #" + execCnt + " to execute command '" + metadata.getCommandKey() + ".");
            metadata.getRetryMetricsCollection(invocationContext.getMethod().getName())
                    .ifPresent(c -> c.getRetriesTotal().inc());
        }

        if (executionStart == null) {
            executionStart = Instant.now();
        }

        try {
            Object returnObject = executeWithHystrix(hystrixCommand, invocationContext, requestContext, metadata);

            if (execCnt > 1) {
                metadata.getRetryMetricsCollection(invocationContext.getMethod().getName())
                        .ifPresent(c -> c.getCallsSucceededRetried().inc());
            } else {
                metadata.getRetryMetricsCollection(invocationContext.getMethod().getName())
                        .ifPresent(c -> c.getCallsSucceededNotRetried().inc());
            }

            return returnObject;
        } catch (Exception e) {
            boolean doRetryOn = Arrays.stream(retryConfig.getRetryOn()).anyMatch(ro -> ro.isInstance(e));
            boolean doAbortOn = Arrays.stream(retryConfig.getAbortOn()).anyMatch(ao -> ao.isInstance(e));

            boolean maxDurationExceeded = executionStart
                    .plus(Duration.of(metadata.getRetry().maxDuration(), metadata.getRetry().durationUnit()))
                    .isBefore(Instant.now());

            if (!doAbortOn && doRetryOn && !maxDurationExceeded &&
                    (retryConfig.getMaxRetries() == -1 || execCnt < retryConfig.getMaxRetries() + 1)) {
                // retry is allowed, execute after delay and jitter
                long jitter = (long)(Math.random() * retryConfig.getJitterInMillis() * 2) -
                        retryConfig.getJitterInMillis();

                TimeUnit.MILLISECONDS.sleep(retryConfig.getDelayInMillis() + jitter);

                return executeWithRetry(hystrixCommand, invocationContext, requestContext, metadata,
                        retryConfig, execCnt + 1, executionStart);
            } else if (metadata.getFallbackHandlerClass() != null || metadata.getFallbackMethod() != null) {
                // retry is not allowed, fallback is set and can be executed
                return FallbackHelper.executeFallback(e, metadata, invocationContext, null);
            } else {
                // retry is not allowed, fallback is not set
                metadata.getRetryMetricsCollection(invocationContext.getMethod().getName())
                        .ifPresent(c -> c.getCallsFailed().inc());
                throw e;
            }
        }
    }

    private Object executeWithHystrix(HystrixCommandConfiguration hystrixCommand, InvocationContext invocationContext,
                                      RequestContext requestContext, ExecutionMetadata metadata) throws Exception {

        KumuluzHystrixGenericCommand cmd = new KumuluzHystrixGenericCommand(hystrixCommand, invocationContext,
                requestContext, metadata);

        try {
            if (metadata.isAsynchronous()) {
                Future queued = cmd.queue();
                return new Future() {
                    @Override
                    public boolean cancel(boolean b) {
                        return queued.cancel(b);
                    }

                    @Override
                    public boolean isCancelled() {
                        return queued.isCancelled();
                    }

                    @Override
                    public boolean isDone() {
                        return queued.isDone();
                    }

                    @Override
                    public Object get() throws InterruptedException, ExecutionException {
                        Object o;
                        try {
                            o = queued.get();

                            if (o instanceof Future) {
                                o = ((Future) o).get();
                            }
                        } catch (ExecutionException e) {
                            Exception processedException = unwrapBulkheadException(e);

                            if (processedException == null && e.getCause() instanceof HystrixRuntimeException) {
                                processedException = processHystrixException((HystrixRuntimeException) e.getCause(),
                                        metadata, invocationContext, cmd);
                            }

                            throw new ExecutionException(processedException);
                        }

                        updateExecutionSuccessfulMetrics(metadata, invocationContext, cmd);

                        return o;
                    }

                    @Override
                    public Object get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {
                        Object o;
                        try {
                            o = queued.get();

                            if (o instanceof Future) {
                                o = ((Future) o).get(l, timeUnit);
                            }
                        } catch (ExecutionException e) {
                            Exception processedException = unwrapBulkheadException(e);

                            if (processedException == null && e.getCause() instanceof HystrixRuntimeException) {
                                processedException = processHystrixException((HystrixRuntimeException) e.getCause(),
                                        metadata, invocationContext, cmd);
                            }

                            throw new ExecutionException(processedException);
                        }

                        updateExecutionSuccessfulMetrics(metadata, invocationContext, cmd);

                        return o;
                    }
                };
            } else {
                Object returnObject = cmd.execute();
                updateExecutionSuccessfulMetrics(metadata, invocationContext, cmd);
                return returnObject;
            }
        } catch (HystrixBadRequestException e) {
            throw (Exception) e.getCause();
        } catch (HystrixRuntimeException e) {
            log.warning("Hystrix runtime exception was thrown because of " + e.getCause().getClass().getName());

            throw processHystrixException(e, metadata, invocationContext, cmd);
        }
    }

    private void markBulkheadRejected(ExecutionMetadata metadata, InvocationContext invocationContext) {
        metadata.getBulkheadMetricsCollection(invocationContext.getMethod().getName())
                .ifPresent(c -> c.getCallsRejected().inc());
        if (metadata.isAsynchronous()) {
            metadata.getBulkheadMetricsCollection(invocationContext.getMethod().getName())
                    .ifPresent(c -> c.getCurrentlyWaiting().decrementAndGet());
        }
    }

    @Override
    public void setPropertyValue(ConfigurationProperty property) {

        log.finest("Received kumuluzee configuration property '" + property.configurationPath() +
                "' with value '" + property.getValue() + "'.");

        if (property.getType() == FaultToleranceType.RETRY) {
            retryManager.updateProperty(property);
        } else {
            configManager.updateProperty(property);
        }
    }

    @Override
    public ConfigurationProperty getPropertyValue(ConfigurationProperty property) {
        return null;
    }

    private HystrixCommandConfiguration getHystrixCommandSetter(ExecutionMetadata metadata) {

        String key = metadata.getIdentifier();

        if (hystrixCommandConfigurations.containsKey(key))
            return hystrixCommandConfigurations.get(key);

        log.finest("Initializing Hystrix command setter for key '" + key + "'.");

        HystrixCommandKey commandKey = getHystrixCommandKey(metadata);
        HystrixCommandGroupKey groupKey = getHystrixCommandGroupKey(metadata);
        HystrixThreadPoolKey threadPoolKey = getHystrixThreadPoolKey(metadata);

        HystrixCommandConfiguration configuration = new HystrixCommandConfiguration(groupKey, commandKey, threadPoolKey);

        hystrixCommandConfigurations.put(key, configuration);

        return configuration;
    }

    private HystrixCommandKey getHystrixCommandKey(ExecutionMetadata metadata) {

        if (hystrixCommandKeys.containsKey(metadata.getIdentifier()))
            return hystrixCommandKeys.get(metadata.getIdentifier());

        log.finest("Initializing Hystrix command key object for key '" + metadata.getIdentifier() + "'.");

        HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(metadata.getCommandKey());

        CommandHystrixConfigurationUtil chcUtil = new CommandHystrixConfigurationUtil(configManager);
        chcUtil.initialize(metadata);

        if (metadata.getRetry() != null)
            retryManager.initializeRetry(metadata);

        hystrixCommandKeys.put(metadata.getIdentifier(), commandKey);

        return commandKey;
    }

    private HystrixCommandGroupKey getHystrixCommandGroupKey(ExecutionMetadata metadata) {

        return HystrixCommandGroupKey.Factory
                .asKey(metadata.getGroupKey());
    }

    private HystrixThreadPoolKey getHystrixThreadPoolKey(ExecutionMetadata metadata) {

        String key = metadata.getGroupKey();

        if (hystrixThreadPoolKeys.containsKey(key))
            return hystrixThreadPoolKeys.get(key);

        log.finest("Initializing Hystrix thread pool key object for key '" + key + "'.");

        if (!metadata.isAsynchronous())
            return null;

        if (metadata.getBulkhead() != null) {
            ThreadPoolHystrixConfigurationUtil tphcUtil = new ThreadPoolHystrixConfigurationUtil(configManager);
            tphcUtil.initialize(metadata);
        }

        HystrixThreadPoolKey threadPoolKey = HystrixThreadPoolKey.Factory.asKey(key);

        hystrixThreadPoolKeys.put(key, threadPoolKey);

        return threadPoolKey;
    }

    private BulkheadException unwrapBulkheadException(ExecutionException e) throws ExecutionException {
        Throwable current = e;
        Throwable previous = null;

        while (previous != current && current != null) {

            if (current instanceof BulkheadException) {
                return (BulkheadException) current;
            }

            previous = current;
            current = current.getCause();
        }

        return null;
    }

    private void updateExecutionSuccessfulMetrics(ExecutionMetadata metadata, InvocationContext invocationContext,
                                                  HystrixCommand cmd) {
        Optional<TimeoutMetricsCollection> metricsCollection = metadata.getTimeoutMetricsCollection(invocationContext
                .getMethod().getName());
        if (metricsCollection.isPresent()) {
            metricsCollection.get().getExecutionDuration().update(cmd.getExecutionTimeInMilliseconds() * 1000000);
            metricsCollection.get().getCallsNotTimedOut().inc();
        }

        metadata.getCbMetricsCollection(invocationContext.getMethod().getName())
                .ifPresent(c -> c.getCallsSucceeded().inc());
    }

    private Exception processHystrixException(HystrixRuntimeException e, ExecutionMetadata metadata,
                                                            InvocationContext invocationContext, HystrixCommand cmd) {

        if (e.getFailureType().equals(HystrixRuntimeException.FailureType.SHORTCIRCUIT)) {
            metadata.getCbMetricsCollection(invocationContext.getMethod().getName())
                    .ifPresent(c -> c.getCallsPrevented().inc());
        } else {
            metadata.getCbMetricsCollection(invocationContext.getMethod().getName())
                    .ifPresent(c -> c.getCallsFailed().inc());
        }

        switch (e.getFailureType()) {
            case TIMEOUT:
                metadata.getTimeoutMetricsCollection(invocationContext.getMethod().getName())
                        .ifPresent(c -> c.getExecutionDuration()
                                .update(cmd.getExecutionTimeInMilliseconds() * 1000000));
                metadata.getTimeoutMetricsCollection(invocationContext.getMethod().getName())
                        .ifPresent(c -> c.getCallsTimedOut().inc());
                return new TimeoutException("Execution timed out.");
            case SHORTCIRCUIT:
                return new CircuitBreakerOpenException("Circuit breaker is in OPEN state.");
            case REJECTED_THREAD_EXECUTION:
                markBulkheadRejected(metadata, invocationContext);
                return new BulkheadException("Thread execution was rejected.");
            case REJECTED_SEMAPHORE_EXECUTION:
                markBulkheadRejected(metadata, invocationContext);
                return new BulkheadException("Semaphore execution was rejected.");
            default:
                return (Exception) e.getCause();
        }
    }
}
