package com.kumuluz.ee.fault.tolerance.exceptions;

/**
 * Created by luka on 25/08/2017.
 */
public class TimeoutException extends FaultToleranceException {

    public TimeoutException() {
        super();
    }

    public TimeoutException(Throwable t){
        super(t) ;
    }

    public TimeoutException(String message){
        super(message) ;
    }

    public TimeoutException(String message, Throwable t) {
        super (message, t);
    }


}
