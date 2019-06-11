# KumuluzEE Fault Tolerance
[![Build Status](https://img.shields.io/travis/kumuluz/kumuluzee-fault-tolerance/master.svg?style=flat)](https://travis-ci.org/kumuluz/kumuluzee-fault-tolerance)

> KumuluzEE Fault Tolerance project for the KumuluzEE microservice framework. 

KumuluzEE Fault Tolerance is a fault tolerance project for the KumuluzEE microservice framework. It provides support 
for fault tolerance and latency tolerance with circuit breaker, bulkhead, timeout, retry and fallback patterns. 
KumuluzEE Fault Tolerance supports basic fault tolerance configuration using annotations. Additionally, 
configuring via KumuluzEE Config is supported. 

KumuluzEE Fault Tolerance has been designed to support modularity with pluggable fault tolerance frameworks. Currently, 
Hystrix implemented by SmallRye is supported. Contributions for other fault tolerance providers are welcome.

KumuluzEE Fault Tolerance fully supports the 
[MicroProfile Fault Tolerance specification](http://microprofile.io/project/eclipse/microprofile-fault-tolerance).

## Usage

You can add the KumuluzEE Fault Tolerance with Hystrix (implemented by SmallRye) by adding the following dependency:
```xml
<dependency>
    <groupId>com.kumuluz.ee.fault.tolerance</groupId>
    <artifactId>kumuluzee-fault-tolerance-smallrye</artifactId>
    <version>${kumuluzee-fault-tolerance.version}</version>
</dependency>
```

To enable fault tolerance patterns using KumuluzEE Fault Tolerance, method in CDI class has to be annotated with
annotations for desired fault tolerance pattern. Currently `@CircuitBreaker`, `@Bulkhead`, `@Timeout`, `@Retry`,
`@Fallback` and `@Asynchronous` are supported. If annotation is added on class, the pattern will be applied on all
methods within the class.

### Fault Tolerance patterns

#### Asynchronous

Asynchronous is an complementary annotation used alongside other fault-tolerance patters. Any method annotated with
`@Asynchronous` will be executed asynchronously on a seperate thread. If the method is annotated with `@Asynchronous` it
must return a `Future` or a `CompletionStage`. When the method is invoked it immediately returns a `Future` or a
`CompletionStage` and the method body gets executed in another thread. When possible it is recommended to return a
`CompletionStage` instead of `Future` since this improves the integration with other fault-tolerance annotations. For
more details see the MP Fault Tolerance specification.

The `@Asynchronous` annotation does not have any parameters.

#### Timeout pattern

Timeout pattern is applied with `@Timeout` annotation. If used on class, all methods will be executed with timeout
pattern. If the execution of the annotated method takes longer than configured in the `@Timeout` annotation the method
will throw a `TimeoutException`. This exception can be additionally handled with other fault-tolerance patterns.

Annotation parameters are as follows:

- __value__ - Timeout value (use with unit for specifing time unit when using annotation). Default value: 1000
- __unit__ - Unit of timeout value. Default value: ChronoUnit.MILLIS

#### Retry pattern

Retry pattern is applied with `@Retry` annotation. If used on class, all methods will be executed with retry pattern.

Annotation parameters are as follows:

- __maxRetries__ - Number of retries if execution does not succeed. If set to -1, it will retry to infinity.
  Default value: 3
- __delay__ - Constant delay added between each retry attempt (use with unit for specifing time unit). Default value: 0
- __delayUnit__ - Unit of constant delay. Default value: ChronoUnit.MILLIS
- __maxDuration__ - Max duration to perform the retry for. Default value: 180000
- __jitter__ - Random jitter added between each retry attempt on top of constant delay (use with unit for specifing time
  unit when using annotation). Default value: 200
- __jitterDelayUnit__ - Unit of jitter. Default value: ChronoUnit.MILLIS
- __retryOn__ - array of Throwable classes at which retry pattern will be applied in case of failed execution.
  Default value: { Exception.class }
- __abortOn__ - array of Throwable classes at which retry pattern will be immediately aborted in case of failed
  execution. Default value: {}

#### Fallback pattern

Fallback pattern is applied with `@Fallback` annotation. If used on class, all methods will be executed with same
defined fallback.
 
Two usage options of `@Fallback` are available:

1. __Usage with class__ - Set class implementing `FallbackHandler` as an annotation value. Method `handle` must return
same type as intercepted method. Class must be a part of CDI. Example:

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

2. __Usage with fallbackMethod__ - provide fallback method name as an annotation `fallbackMethod` parameter. Method must
exists in same class as intercepted method. Return type and parameter types must be the same as in intercepted method.
Example: 

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

#### Circuit breaker pattern

Circuit breaker pattern is applied with `@CircuitBreaker` annotation. If used on class, all methods will be executed
with circuit breaker pattern.

Annotation parameters are as follows:

- __failOn__ - Array of Throwable classes marking execution as failed. Default value: { Throwable.class }
- __delay__ - Wait time circuit breaker will wait before executing next request when circuit is open (use with delayUnit 
for specifing time unit when using annotation). Default value: 5000
- __delayUnit__ - Unit of delay. Default value: ChronoUnit.MILLIS
- __requestVolumeThreshold__ - Number of minimum executions within rolling window needed to trip the circuit.
  Default value: 20
- __failureRatio__ - Failure ratio that causes circuit to trip open. Default value: 0.5
- __successThreshold__ - Number of successful executions required to close the circuit back. Default value: 1

#### Bulkhead pattern

Bulkhead pattern is applied with `@Bulkhead` annotation. If bulkhead pattern is applied on class, all methods will be
executed with bulkhead patterns.

By default, bulkhead pattern is semaphore executed. If used in combination with `@Asynchronous` annotation, thread
execution is applied. Thread execution allows the configuration of the waiting queue size in addition to the number of
concurrent parallel executions whereas semaphored execution supports only the latter.

Annotation parameters are as follows:
  
- __value__ - sets number of concurrent parallel executions. Default value: 10
- __waitingTaskQueue__ - sets queue size (only in thread execution). Default value: 10
 
### KumuluzEE Configuration

KumuluzEE Fault Tolerance can be configured via KumuluzEE Config. To learn more about KumuluzEE Config please visit 
[KumuluzEE Config wiki page](https://github.com/kumuluz/kumuluzee/wiki/Configuration) and the 
[KumuluzEE Config project](https://github.com/kumuluz/kumuluzee-config).

Any property of any fault tolerance annotation can be adjusted using the KumuluzEE configuration. For example if
annotation `@Retry` is present on the method `exampleMethod` in class `com.example.beans.ExampleBean` the following
configuration will set the `maxRetries` property of the annotation to 15.

config.yml:

```yaml
kumuluzee:
  fault-tolerance:
    annotation-overrides:
      - class: com.example.beans.ExampleBean
        method: exampleMethod
        annotation: retry
        parameters:
          max-retries: 15
```

The rules are as follows:

- The `class` property should be the fully-qualified name of the class.
- The `annotation` property should be hyphen-case annotation name (e.g. `circuit-breaker`).
- The parameter names should be hyphen-case.

In addition to annotation parameters the `enabled` parameter can be specified too. If set to `false` the annotation is
disabled. For example in order to disable the `@Retry` annotation in the example above use the following configuration:

```yaml
kumuluzee:
  fault-tolerance:
    annotation-overrides:
      - class: com.example.beans.ExampleBean
        method: exampleMethod
        annotation: retry
        parameters:
          enabled: false
```

In order to override parameters of an annotation present on class simply leave out the `method` key. For example in
order to set the `delay` property to 100 for the annotation `@Retry` declared on the `com.example.beans.ExampleBean`
class use the following configuration:

```yaml
kumuluzee:
  fault-tolerance:
    annotation-overrides:
      - class: com.example.beans.ExampleBean
        annotation: retry
        parameters:
          delay: 100
```

__NOTE:__ In order to override annotation parameters the annotation must actually exist. For example if the
`com.example.beans.ExampleBean` didn't have the `@Retry` annotation in the example above the configuration would not
work (even if the methods defined in the class had the `@Retry` annotation).

Another option available is specifying global overrides. For example the following config will disable all `@Fallback`
annotations and set the `maxRetries` argument of all `@Retry` annotations to 10. Again, note that annotation and
parameter names in keys need to be in hyphen-case.

```yaml
kumuluzee:
  fault-tolerance:
    fallback:
      enabled: false
    retry:
      max-retries: 10
```

Presented configuration keys are one-to-one mapping to the keys defined in MP Fault Tolerance specification. Mapping is
being done in order to better fit the KumuluzEE configuration model. If desired the keys defined in the specification
can still be used and will work as expected.

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

Note that since 2.0 release the in-house developed Hystrix extension was replaced by the extension implemented by
SmallRye. The previous in-house developed extension will not receive any more updates except fixes for critical bugs.
The documentation for older releases is still accessible
[here](https://github.com/kumuluz/kumuluzee-fault-tolerance/tree/master/hystrix).

## Contribute

See the [contributing docs](https://github.com/kumuluz/kumuluzee-fault-tolerance/blob/master/CONTRIBUTING.md)

When submitting an issue, please follow the 
[guidelines](https://github.com/kumuluz/kumuluzee-fault-tolerance/blob/master/CONTRIBUTING.md#bugs).

When submitting a bugfix, write a test that exposes the bug and fails before applying your fix. Submit the test 
alongside the fix.

When submitting a new feature, add tests that cover the feature.

## License

MIT
