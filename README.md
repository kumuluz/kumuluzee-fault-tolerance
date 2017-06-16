# KumuluzEE Circuit Breaker
[![Build Status](https://img.shields.io/travis/kumuluz/kumuluzee-circuit-breaker/master.svg?style=flat)](https://travis-ci.org/kumuluz/kumuluzee-discovery)

> KumuluzEE Circuit Breaker extension for the Kumuluz EE microservice framework. 

KumuluzEE Circuit Breaker is a circuit breaker extension for the KumuluzEE microservice framework. It provides support 
for fault tolerance and latency tolerance using the circuit breaker pattern. KumuluzEE Circuit Breaker supports basic 
circuit breaker configuration using annotations. Additionaly, configuring circuit breakers via KumuluzEE Config is supported. 

KumuluzEE Circuit Breaker has been designed to support modularity with pluggable circuit breaker frameworks. Currently, 
Hystrix is supported. Contributions for other circuit breaker providers are welcome.

## Usage

You can add the KumuluzEE Circuit Breaker with Hystrix by adding the following dependency:
```xml
<dependency>
    <groupId>com.kumuluz.ee.circuit.breaker</groupId>
    <artifactId>kumuluzee-circuit-breaker-hystrix</artifactId>
    <version>1.0.0</version>
</dependency>
```

To enable circuit breaker using KumuluzEE Circuit Breaker, CDI class has to be annotated with `@EnableCircuitBreaker`.
Each method that will be executed with circuit breaker has to be annotated with `@CircuitBreaker` annotation.

KumuluzEE Circuit Breaker will intercept the method execution and proceed the execution within the circuit breaker to track and monitor success, failures, timeouts, etc.

### Circuit Breaker basic configuration

KumuluzEE Circuit Breaker properties can be configured using annotations and/or KumuluzEE Config. `@CircuitBreaker` 
annotation provides several parameters that can set basic circuit breaker behavior:

- key: circuit breaker command key. Default value is method name.
- fallbackMethod: name of the fallback method that will be executed in case of failure. Provided fallback method must be public. In addition, it must take same parameters and have the same return type as the method annotated for the circuit breaker execution.
- skipFallbackOn: array of Throwable classes for which fallback execution will not occur in case of a failure. Exceptions defined in throws method clause will additionally be added to the array when the circuit breaker call will begin. By default, RuntimeException class is added to array.
- group: command group key. Annotation `@CircuitBreakerGroup` can also set the group key on the method or class. If used on a method, it will override the group set by the `@CircuitBreaker` property. If not provided, the class name will be used as the command's group key.
- timeout: time limit for the execution before the fallback is called. Default value is 1000.
- timeoutUnit: time unit for the timeout setting. Default unit is milliseconds.
- requestThreshold: minimum number of requests that will trip the circuit. Default value is 20.
- failureThreshold: error percentage above which the circuit will trip open. Default value is 50.
- openCircuitWait: amount of time the circuit breaker will reject all requests after tripping the circuit before allowing attempts again. Default value is 5000.
- openCircuitWaitUnit: time unit for openCircuitWait setting. Default unit is milliseconds.
- forceClosed: forces circuit to closed state. Default value is false.
- forceOpen: forces circuit to open state. Default value is false.

If any properties are set through the annotations, they cannot be overridden with any other type of configuration.

Example of using KumuluzEE Circuit Breaker:

```java
@RequestScoped
@EnableCircuitBreaker
@CircuitBreakerGroup("customers")
public class CustomersBean {

    @CircuitBreaker(key = "find-customers", fallbackMethod = "findCustomersFallback", skipFallbackOn = {})
    public List<Customer> findCustomers(String query) {
        // ...
    }
    
    public List<Customer> findCustomersFallback(String query) {
        return new ArrayList<>();
    }

}
```

**NOTE**: Try to avoid usage of `@RequestScoped` CDI injection into CDI annotated with `@EnableCircuitBreaker`. Default behaviour of KumuluzEE Circuit Breaker on Hystrix platform
is to execute circuit breaker in isolated thread. This causes the context of injected CDI to be inactive (it's injected dependencies are not initiated). This can be avoid by 
using [semaphore isolated execution](https://github.com/Netflix/Hystrix/wiki/How-it-Works#Isolation). No such issues were detected using the `@ApplicationScoped` 
CDI.

 
### KumuluzEE Configuration

KumuluzEE Circuit Breaker can be configured via KumuluzEE Config. To learn more about KumuluzEE Config please visit [KumuluzEE Config wiki page](https://github.com/kumuluz/kumuluzee/wiki/Configuration) and the [KumuluzEE Config extension]( https://github.com/kumuluz/kumuluzee-config).

Basic circuit breaker properties (except for the command key) can be set using the key format: 
`circuit-breaker.commands.<command-key>.<property-name>`. 
For time units, the desired time unit can be provided after the numeric value (i.e. 3s). Minutes [m], seconds [s], milliseconds [ms] and nanoseconds [ns] are supported.

Example of config.yml for setting the basic properties using KumuluzEE Config:

```yml
circuit-breaker:
  commands:
    get:
      timeout: 2500ms
      request-threshold: 30
      failure-threshold: 20
      open-circuit-wait: 3s
      force-closed: false
      force-open: false
      group: put
```

### Configuring circuit breaker framework command specific properties

KumuluzEE Circuit Breaker supports configuring circuit breaker framework specific properties. If the property for the command is not already among basic circuit breaker properties, it can be set using the key format: 
`circuit-breaker.commands.<command-key>.<framework-name>.<property-name>`.

Example of config.yml for setting Hystrix specific command properties using KumuluzEE Config:

```yml
circuit-breaker:
  commands:
    get:
      timeout: 2500ms
      #...
      hystrix:
        execution-strategy: semaphore
        semaphore-execution-max-concurrent-requests: 5
        semaphore-fallback-max-concurrent-requests: 2
        timeout-enabled: true
        metrics-rolling-statistical-window: 5s
        metrics-rolling-statistical-window-buckets: 5
```

### Watching for property value change

With KumuluzEE Config, usage of configuration servers is supported. Updating circuit breaker configuration properties is an important feature in order to be able to adapt to different loads on services. KumuluzEE Circuit Breaker supports watching for property changes in configuration server by setting configuration `kumuluzee.circuit-breaker.watch-enabled` to true.

By default, all properties that can be changed after initialization in circuit breaker framework can be watched.
With libraries like Hystrix, which provide a lot of configuration parameters, it is recommended to limit watched properties. We recommend using watches only for those properties that might change and for which it makes sense to react on the change in runtime. 
Specifying watched properties can be done by setting 
`kumuluzee.circuit-breaker.watch-properties` with comma separated property names that can be watched. 

Example of config.yml for setting property value watching using KumuluzEE Config:

```yml
kumuluzee:
  circuit-breaker:
    watch-enabled: true
    watch-properties: force-closed,request-threshold,failure-threshold
```

**NOTE**: When setting property value using annotations, watches will not be triggered, since any 
configuration made by KumuluzEE Config cannot override values set by the annotations.

### Framework specific configurations

With certain circuit breaker frameworks (i.e. Hystrix), there are also configurations for other property types beside the commands available. Such important property types are the thread pools. These properties can also be configured in KumuluzEE Circuit Breaker using KumuluzEE Config extension by using configuration key format `circuit-breaker.<framework-name>.<configuration-type>.<configuration-type-name>.<property-name>`.

Example of config.yml for setting Hystrix thread pool properties using KumuluzEE Config:

```yml
circuit-breaker:
  hystrix:
    thread-pools:
      default:
        threads-core: 6
        threads-max: 8
        threads-diverge-from-core: true
```

Hystrix uses group key as the default configuration for setting command's thread pool key. The same goes for KumuluzEE Circuit Breaker extension for Hystrix. In order to set the command thread pool key different than the command group key, set the framework command specific property with the `thread-pool` key.  

## Changelog

Recent changes can be viewed on Github on the [Releases Page](https://github.com/kumuluz/kumuluzee-circuit-breaker/releases)

## Contribute

See the [contributing docs](https://github.com/kumuluz/kumuluzee-circuit-breaker/blob/master/CONTRIBUTING.md)

When submitting an issue, please follow the 
[guidelines](https://github.com/kumuluz/kumuluzee-circuit-breaker/blob/master/CONTRIBUTING.md#bugs).

When submitting a bugfix, write a test that exposes the bug and fails before applying your fix. Submit the test 
alongside the fix.

When submitting a new feature, add tests that cover the feature.

## License

MIT
