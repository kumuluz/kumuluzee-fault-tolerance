package com.kumuluz.ee.fault.tolerance.exceptions;

/**
 * Created by luka on 25/08/2017.
 */
public class CircuitBreakerOpenException extends FaultToleranceException {

    public CircuitBreakerOpenException() {
        super();
    }

    public CircuitBreakerOpenException(Throwable t){
        super(t) ;
    }

    public CircuitBreakerOpenException(String message){
        super(message) ;
    }

    public CircuitBreakerOpenException(String message, Throwable t) {
        super (message, t);
    }


}