package com.kumuluz.ee.fault.tolerance.exceptions;

/**
 * Created by luka on 25/08/2017.
 */
public class BulkheadException extends FaultToleranceException {

    private static final long serialVersionUID = 3569768756115160625L;

    public BulkheadException() {
        super();
    }

    public BulkheadException(Throwable t){
        super(t) ;
    }

    public BulkheadException(String message){
        super(message) ;
    }

    public BulkheadException(String message, Throwable t) {
        super (message, t);
    }


}