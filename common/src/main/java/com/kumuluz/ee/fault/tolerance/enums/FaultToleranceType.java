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
package com.kumuluz.ee.fault.tolerance.enums;

/**
 * Enum type for fault tolerance pattern type.
 *
 * @author Luka Šarc
 */
public enum FaultToleranceType {

    ASYNCHRONOUS("asynchronous"),
    BULKHEAD("bulkhead"),
    TIMEOUT("timeout"),
    FALLBACK("fallback"),
    CIRCUIT_BREAKER("circuit-breaker");

    private final String key;

    FaultToleranceType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static FaultToleranceType toEnum(String str) {

        if (str.equals(ASYNCHRONOUS.getKey()))
            return ASYNCHRONOUS;
        else if (str.equals(BULKHEAD.getKey()))
            return BULKHEAD;
        else if (str.equals(TIMEOUT.getKey()))
            return TIMEOUT;
        else if (str.equals(FALLBACK.getKey()))
            return FALLBACK;
        else if (str.equals(CIRCUIT_BREAKER.getKey()))
            return CIRCUIT_BREAKER;
        else
            return null;
    }
}
