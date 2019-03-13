# KumuluzEE Fault Tolerance
[![Build Status](https://img.shields.io/travis/kumuluz/kumuluzee-fault-tolerance/master.svg?style=flat)](https://travis-ci.org/kumuluz/kumuluzee-fault-tolerance)

> KumuluzEE Fault Tolerance project for the KumuluzEE microservice framework. 

KumuluzEE Fault Tolerance is a fault tolerance project for the KumuluzEE microservice framework. It provides support 
for fault tolerance and latency tolerance with circuit breaker, bulkhead, timeout, retry and fallback patterns. 
KumuluzEE Fault Tolerance supports basic fault tolerance configuration using annotations. Additionaly, 
configuring via KumuluzEE Config is supported. 

KumuluzEE Fault Tolerance has been designed to support modularity with pluggable fault tolerance frameworks. Currently, 
Hystrix is supported. Contributions for other fault tolerance providers are welcome.

KumuluzEE Fault Tolerance fully supports the 
[MicroProfile Fault Tolerance specification](http://microprofile.io/project/eclipse/microprofile-fault-tolerance).

## Usage

You can add the KumuluzEE Fault Tolerance with Hystrix by adding the following dependency:
```xml
<dependency>
    <groupId>com.kumuluz.ee.fault.tolerance</groupId>
    <artifactId>kumuluzee-fault-tolerance-hystrix</artifactId>
    <version>${kumuluzee-fault-tolerance.version}</version>
</dependency>
```

To enable fault tolerance patterns using KumuluzEE Fault Tolerance, method in CDI class has to be annotated with annotations for
desired fault tolerance pattern. Currently `@CircuitBreaker`, `@Bulkhead`, `@Timeout`, `@Retry` and `@Fallback` are supported.
If annotation is added on class, the pattern will be applied on all methods within class.

KumuluzEE Fault Tolerance will intercept the method execution and proceed the execution within the fault tolerance to 
track and monitor success, failures, timeouts, etc. Currently, `@Bulkhead`, `@Timeout`, `@Retry` and `@Fallback` cannot
be used as standalone annotations. They can only be used in conjunction with `@CircuitBreaker`. In future,
we will add additional usage possibilities.

### Fault Tolerance basic configuration

All fault tolerance executions are executed as a commands withing groups. Each command and group is identified with key.
By default, `<class_name>-<method_name>` is used as a key for command and `<class_name>` is used as a key for group.
Default settings can be overridden with `@CommandKey` and `@GroupKey` annotations.

KumuluzEE Fault Tolerance properties can be configured using annotations and/or KumuluzEE Config.

`@CircuitBreaker`, `@Timeout`, `@Retry` and `@Fallback` setting are binded to command. `@Bulkhead` is binded to groups. If 
bulkhead pattern is used on single method, default group key will be set to `<class-name>-<method-name>`. It can
be overriden with `@GroupKey` annotation. If bulkhead pattern is applied on class, all methods will be executed within the
same bulkhead group key and limitations defined within bulkhead pattern will be applied to all commands within the group.

Example of using KumuluzEE Fault Tolerance circuit breaker pattern with bulkhead, timeout and fallback defined:

```java
@RequestScoped
@CircuitBreaker
@Bulkhead
@GroupKey("customers")
public class CustomersBean {

    @Timeout(value = 2, unit = ChronoUnit.SECONDS)
    @CommandKey("find-customers")
    public List<Customer> findCustomers(String query) {
        // ...
    }
}
``` 
 
### KumuluzEE Configuration

KumuluzEE Fault Tolerance can be configured via KumuluzEE Config. To learn more about KumuluzEE Config please visit 
[KumuluzEE Config wiki page](https://github.com/kumuluz/kumuluzee/wiki/Configuration) and the 
[KumuluzEE Config project](https://github.com/kumuluz/kumuluzee-config).

Fault tolerance command-specific properties are applied only to command under specified group and command key.
They can be set using key format: 

```
fault-tolerance.<group-key>.<command-key>.<fault-tolerance-pattern>.<property-name-path>
```

Group-specific settings are applied to all commands within group except for bulkhead pattern which is applied to a
group by design. Group-specific settings can be set using key format:

```
fault-tolerance.<group-key>.<fault-tolerance-pattern>.<property-name-path>
```

Global settings are also supported using the key format:

```
fault-tolerance.<fault-tolerance-pattern>.<property-name-path>
```

When multiple settings are present, the command-specific setting takes precedence over group-specific setting, which is
followed by global setting.

For time units, the desired time unit can be provided after the numeric value (i.e. 3s). Minutes [m], seconds [s], 
milliseconds [ms] and nanoseconds [ns] are supported.

Example of config.yml` for setting properties using KumuluzEE Config:

```yml
fault-tolerance:
  circuit-breaker:
    delay: 3s
  customers:
    bulkhead:
      value: 5
    find-customers:
      timeout: 
        value: 2500ms
      circuit-breaker:
        request-volume-threshold: 30
        failure-ratio: 0.3
        metrics:
          rolling-window:
            size: 8s
            buckets: 4
```

### Watching for property value change

With KumuluzEE Config, usage of configuration servers is supported. Updating circuit breaker configuration properties 
is an important feature in order to be able to adapt to different loads on services. KumuluzEE Fault Tolerance
supports watching for property changes in configuration server by setting configuration 
`fault-tolerance.config.watch-enabled` to true.

By default, all properties that can be changed after initialization in fault tolerance framework can be watched.
With libraries like Hystrix, which provide a lot of configuration parameters, it is recommended to limit watched properties. We recommend using watches only for those properties that might change and for which it makes sense to react on the change in runtime. 
Specifying watched properties can be done by setting 
`fault-tolerance.config.watch-properties` with comma separated key paths. Use name of fault tolerance pattern with property
 key path.

Example of config.yml for setting property value watching using KumuluzEE Config:

```yml
fault-tolerance:
  config:
    watch-enabled: true
    watch-properties: timeout.value,circuit-breaker.failure-ratio
```

**NOTE**: When setting properties on multiple levels (global, group-specific, command-specific), only the applied key path
at first execution for each command (or group in case of bulkhead pattern) will be watched.

### Fault Tolerance patterns

#### Circuit breaker pattern

Circuit breaker pattern is applied with `@CircuitBreaker` annotation. If used on class, all methods will be executed with 
circuit breaker pattern. 

Common settings, available via annotation or KumuluzEE Config can be applied:

- __delay__ - wait time circuit breaker will wait before executing next request when circuit is open (use with delayUnit 
for specifing time unit when using annotation).
- __requestVolumeThreshold (config:  request-volume-threshold)__ - number of minimum executions within rolling window 
needed to trip the circuit.
- __failureRatio (config: failure-ratio)__ - failure ratio that causes circuit to trip open.
- __failOn__ - array of Throwable classes marking execution as failed (can only be set with annotation).

Some additional Hystrix specific properties are available using the KumuluzEE Config settings:

- __metrics.rolling-window.size__ - sets size of Hystrix metrics rolling window in time.
- __metrics.rolling-window.buckets__ - sets number of rolling window buckets.
- __metrics.rolling-percentile.enabled__ - enables Hystrix rolling percentile metrics.
- __metrics.rolling-percentile.size__ - sets size of Hystrix metrics rolling percentile window in time.
- __metrics.rolling-percentile.buckets__ - sets number of rolling percentile window buckets.
- __metrics.rolling-percentile.bucket-size__ - sets maximum rolling percentile bucket size.
- __metrics.health-interval__ - sets interval for calculating error percentage and other health metrics in Hystrix.
- __interrupt.on-timeout__ - sets whether to interrupt the thread on timeout when using thread execution.
- __interrupt.on-cancel__ - sets whether to interrupt the thread on cancelation when using thread execution.
- __log.enabled__ - enables Hystrix request log.
 
KumuluzEE Fault Tolerance supports two implementations of circuit breaker. The implementation can be selected using the
KumuluzEE Config key:

- __circuit-breaker-type__ - selects the circuit breaker implementation - can be `HYSTRIX` (default) or
  `SUCCESS_THRESHOLD`.

The default value (`HYSTRIX`) uses the Hystrix implementation of circuit breaker, which does not support the
`successThreshold` parameter. It can also violate the `failureRatio` parameter, since it updates error percentage on a
regular interval (configurable with `metrics.health-interval`) and not on every invocation. The alternative implementation
(`SUCCESS_THRESHOLD`) supports the `successThreshold` parameter and always respects the `failureRatio` parameter since
it calculates the error percentage before every invocation. However, this calculation can be inefficient in high
throughput applications. We recommend the usage of the default value `HYSTRIX` implementation, since it offers higher
scalability. The drawbacks of `HYSTRIX` implementation can be easily mitigated by properly tuning the configuration.

 #### Bulkhead pattern
 
 Bulkhead pattern is applied with `@Bulkhead` annotation. Bulkhead pattern is binded to group and NOT to commands as other
 patterns. If bulkhead pattern is applied on class, all methods will be executed within the same bulkhead group key 
 and limitations defined within bulkhead pattern will be applied to all commands within the group.
 
 By default, bulkhead pattern is semaphore executed. If used in combination with `@Asynchronous` annotation, thread
 execution is applied. Semaphored bulkhead execution will automatically be applied when using circuit breaker pattern.
 
 Common settings, available via annotation or KumuluzEE Config can be applied:
  
- __value (config: value)__ - sets number of concurrent parallel executions.
- __waitingTaskQueue (config: waiting-task-queue)__ - sets queue size (only in thread execution).

Some additional Hystrix specific properties for thread execution are available using the KumuluzEE Config settings:
- __metrics.rolling-window.size__ - sets size of Hystrix metrics rolling window in time.
- __metrics.rolling-window.buckets__ - sets number of rolling window buckets.
- __keep-alive__ - sets minimum keep alive time of thread.
  
#### Timeout pattern

Timeout pattern is applied with `@Timeout` annotation. If used on class, all methods will be executed with 
timeout pattern. 

Common settings, available via annotation or KumuluzEE Config can be applied:

- __value (config: value)__ - timeout value (use with unit for specifing time unit when using annotation).

#### Retry pattern

Retry pattern is applied with `@Retry` annotation. If used on class, all methods will be executed with 
retry pattern. 

Common settings, available via annotation or KumuluzEE Config can be applied:

- __maxRetries (config: max-retries)__ - number of retries if execution does not succeed. If set to -1, it will retry
to infinity.
- __delay (config: delay)__ - constant delay added between each retry attempt (use with unit for specifing time unit 
when using annotation).
- __jitter (config: jitter)__ - random jitter added between each retry attempt on top of constant delay 
(use with unit for specifing time unit when using annotation).
- __retryOn__ - array of Throwable classes at which retry pattern will be applied in case of failed execution
 (can only be set with annotation).
- __abortOn__ - array of Throwable classes at which retry pattern will be immediately aborted in case of failed 
execution (can only be set with annotation).

#### Fallback pattern

Fallback pattern is applied with `@Fallback` annotation. If used on class, all methods will be executed with 
same defined fallback.
 
Fallback can be defined only using the annotation. Two usage options are available:
1. __Usage with class__ - set class implementing _FallbackHandler_. Method _handle_ must return same type as 
intercepted method. Class must be a CDI. Example:

```java
@RequestScoped
@GroupKey("customers")
public class CustomersBean {

    @CircuitBreaker
    @Timeout(value = 2, unit = ChronoUnit.SECONDS)
    @Fallback(FindCustomerFallback.class)
    @CommandKey("find-customers")
    public List<Customer> findCustomers(String query) {
        // ...
    }
}
```

```java
@RequestScoped
public class FindCustomerFallback implements FallbackHandler<List<Customer>> {
    
    @Override
    public List<Customer> handle(ExecutionContext executionContext) {
        return new ArrayList<>();
    }
}
``` 

2. __Usage with fallbackMethod__ - provide fallback method name. Method must exists in same class as intercepted method.
Return type and parameter types must be the same as in intercepted method. Example: 

```java
@RequestScoped
@GroupKey("customers")
public class CustomersBean {

    @CircuitBreaker
    @Timeout(value = 2, unit = ChronoUnit.SECONDS)
    @Fallback(fallbackMethod = "findCustomersFallback")
    @CommandKey("find-customers")
    public List<Customer> findCustomers(String query) {
        // ...
    }
    
    public List<Customer> findCustomersFallback(String query) {
        return new ArrayList<>();
    }

}
``` 

### Integration with KumuluzEE Metrics

KumuluzEE Fault Tolerance includes integration with the Metrics extension and adds important metrics of the fault
tolerance patterns to the registry. To enable this integration, simply add the following dependency to your pom.xml:

```xml
<dependency>
    <groupId>com.kumuluz.ee.metrics</groupId>
    <artifactId>kumuluzee-metrics-core</artifactId>
    <version>${kumuluzee-metrics.version}</version>
</dependency>
```

Some of the metrics included are:

- histogram of execution times of methods annotated with `@Timeout`
- number of times the method annotated with `@Retry` was retried
- number of calls prevented by the circuit breaker
- number of executions in queue for methods, annotated with `@Bulkhead` and `@Asynchronous`
- number of times the fallback method has been executed

For description of all metrics, check out the MicroProfile Fault Tolerance specification.

## Changelog

Recent changes can be viewed on Github on the [Releases Page](https://github.com/kumuluz/kumuluzee-fault-tolerance/releases)

## Contribute

See the [contributing docs](https://github.com/kumuluz/kumuluzee-fault-tolerance/blob/master/CONTRIBUTING.md)

When submitting an issue, please follow the 
[guidelines](https://github.com/kumuluz/kumuluzee-fault-tolerance/blob/master/CONTRIBUTING.md#bugs).

When submitting a bugfix, write a test that exposes the bug and fails before applying your fix. Submit the test 
alongside the fix.

When submitting a new feature, add tests that cover the feature.

## License

MIT
