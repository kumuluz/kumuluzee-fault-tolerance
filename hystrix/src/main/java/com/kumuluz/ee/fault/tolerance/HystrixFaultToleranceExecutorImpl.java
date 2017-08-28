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

import com.kumuluz.ee.fault.tolerance.commands.HystrixGenericCommand;
import com.kumuluz.ee.fault.tolerance.configurations.CommandConfigurationUtil;
import com.kumuluz.ee.fault.tolerance.configurations.HystrixFaultToleranceConfigurationManager;
import com.kumuluz.ee.fault.tolerance.configurations.ThreadPoolConfigurationUtil;
import com.kumuluz.ee.fault.tolerance.interfaces.FaultToleranceExecutor;
import com.kumuluz.ee.fault.tolerance.models.ConfigurationProperty;
import com.kumuluz.ee.fault.tolerance.models.ExecutionMetadata;
import com.kumuluz.ee.fault.tolerance.utils.FaultToleranceUtilImpl;
import com.netflix.hystrix.*;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.jboss.weld.context.RequestContext;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Hystrix implementation of circuit breaker executor
 *
 * @author Luka Šarc
 */
@RequestScoped
public class HystrixFaultToleranceExecutorImpl implements FaultToleranceExecutor {

    private static final String NAME = "hystrix";

    private static final Logger log = Logger.getLogger(HystrixFaultToleranceExecutorImpl.class.getName());

    private static HashMap<String, HystrixCommand.Setter> hystrixCommandSetters = new HashMap<>();
    private static HashMap<String, HystrixCommandKey> hystrixCommandKeys = new HashMap<>();
    private static HashMap<String, HystrixThreadPoolKey> hystrixThreadPoolKeys = new HashMap<>();

    @Inject
    private FaultToleranceUtilImpl faultToleranceUtil;

    @Inject
    private HystrixFaultToleranceConfigurationManager configManager;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Object execute(InvocationContext invocationContext, RequestContext requestContext, ExecutionMetadata metadata) throws Exception {

        HystrixCommand.Setter hystrixCommand = getHystrixCommandSetter(metadata);

        HystrixGenericCommand cmd = new HystrixGenericCommand(hystrixCommand, invocationContext, requestContext, metadata);

        try {
            return cmd.execute();
        } catch (HystrixBadRequestException e) {
            throw (Exception) e.getCause();
        } catch (HystrixRuntimeException e) {
            log.warning("Hystrix runtime exception was thrown because of " + e.getCause().getClass().getName());
            throw (Exception) e.getCause();
        }
    }

    @Override
    public void setPropertyValue(ConfigurationProperty property) {

        log.finest("Received kumuluzee configuration property '" + property.configurationPath() +
                "' with value '" + property.getValue() + "'.");

        configManager.updateProperty(property);
    }

    @Override
    public ConfigurationProperty getPropertyValue(ConfigurationProperty property) {
        return null;
    }

    private HystrixCommand.Setter getHystrixCommandSetter(ExecutionMetadata metadata) {

        String key = metadata.getGroupKey() + "-" + metadata.getCommandKey();

        if (hystrixCommandSetters.containsKey(key))
            return hystrixCommandSetters.get(key);

        log.finest("Initializing Hystrix command setter for key '" + key + "'.");

        HystrixCommandKey commandKey = getHystrixCommandKey(metadata);
        HystrixCommandGroupKey groupKey = getHystrixCommandGroupKey(metadata);
        HystrixThreadPoolKey threadPoolKey = getHystrixThreadPoolKey(metadata);

        HystrixCommand.Setter setter = HystrixCommand.Setter
                .withGroupKey(groupKey)
                .andThreadPoolKey(threadPoolKey)
                .andCommandKey(commandKey);

        hystrixCommandSetters.put(key, setter);

        return setter;
    }

    private HystrixCommandKey getHystrixCommandKey(ExecutionMetadata metadata) {

        String key = metadata.getCommandKey();

        if (hystrixCommandKeys.containsKey(key))
            return hystrixCommandKeys.get(key);

        log.finest("Initializing Hystrix command key object for key '" + key + "'.");

        HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(key);

        CommandConfigurationUtil ccUtil = new CommandConfigurationUtil(configManager);
        ccUtil.initialize(metadata);

        hystrixCommandKeys.put(key, commandKey);

        return commandKey;
    }

    private HystrixCommandGroupKey getHystrixCommandGroupKey(ExecutionMetadata metadata) {

        HystrixCommandGroupKey commandGroupKey = HystrixCommandGroupKey.Factory
                .asKey(metadata.getGroupKey());

        return commandGroupKey;
    }

    private HystrixThreadPoolKey getHystrixThreadPoolKey(ExecutionMetadata metadata) {

        String key = metadata.getGroupKey();

        if (hystrixThreadPoolKeys.containsKey(key))
            return hystrixThreadPoolKeys.get(key);

        log.finest("Initializing Hystrix thread pool key object for key '" + key + "'.");

        if (!metadata.isAsynchronous())
            return null;

        if (metadata.getBulkhead() != null) {
            ThreadPoolConfigurationUtil tpcUtil = new ThreadPoolConfigurationUtil(configManager);
            tpcUtil.initialize(metadata);
        }

        HystrixThreadPoolKey threadPoolKey = HystrixThreadPoolKey.Factory.asKey(key);

        hystrixThreadPoolKeys.put(key, threadPoolKey);

        return threadPoolKey;
    }

    private void setHystrixProperty(String key, Object value) {
        com.netflix.config.ConfigurationManager.getConfigInstance().setProperty(key, value);
    }

    private Object getHystrixProperty(String key) {
        return com.netflix.config.ConfigurationManager.getConfigInstance().getProperty(key);
    }
}
