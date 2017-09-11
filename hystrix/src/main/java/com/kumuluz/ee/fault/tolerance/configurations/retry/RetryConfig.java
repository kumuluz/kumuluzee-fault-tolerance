package com.kumuluz.ee.fault.tolerance.configurations.retry;

/**
 * Configuration model class for retry pattern
 *
 * @author Luka Šarc
 * @since 1.0.0
 */
public class RetryConfig {

    private final Class<? extends Throwable>[] retryOn;
    private final Class<? extends Throwable>[] abortOn;

    private int maxRetries;
    private long delayInMillis;
    private long jitterInMillis;

    public RetryConfig(Class<? extends Throwable>[] retryOn, Class<? extends Throwable>[] abortOn) {
        this.retryOn = retryOn;
        this.abortOn = abortOn;
    }

    public Class<? extends Throwable>[] getRetryOn() {
        return retryOn;
    }

    public Class<? extends Throwable>[] getAbortOn() {
        return abortOn;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getDelayInMillis() {
        return delayInMillis;
    }

    public void setDelayInMillis(long delayInMillis) {
        this.delayInMillis = delayInMillis;
    }

    public long getJitterInMillis() {
        return jitterInMillis;
    }

    public void setJitterInMillis(long jitterInMillis) {
        this.jitterInMillis = jitterInMillis;
    }
}
