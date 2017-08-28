package com.kumuluz.ee.fault.tolerance.exceptions;

/**
 * Created by luka on 25/08/2017.
 */
public class FaultToleranceException extends RuntimeException {

    public FaultToleranceException() {
        super();
    }

    public FaultToleranceException(Throwable t){
        super(t) ;
    }

    public FaultToleranceException(String message){
        super(message) ;
    }

    public FaultToleranceException(String message, Throwable t) {
        super (message, t);
    }

}