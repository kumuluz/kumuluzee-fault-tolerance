package com.kumuluz.ee.fault.tolerance.exceptions;

/**
 * Created by luka on 25/08/2017.
 */
public class FaultToleranceConfigException extends FaultToleranceException {

    public FaultToleranceConfigException() {
        super();
    }

    public FaultToleranceConfigException(Throwable t){
        super(t) ;
    }

    public FaultToleranceConfigException(String message){
        super(message) ;
    }

    public FaultToleranceConfigException(String message, Throwable t) {
        super (message, t);
    }

}