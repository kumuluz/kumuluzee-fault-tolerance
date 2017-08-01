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
package com.kumuluz.ee.fault.tolerance.models;

import com.kumuluz.ee.fault.tolerance.utils.CircuitBreakerHelper;
import com.kumuluz.ee.fault.tolerance.utils.CircuitBreakerUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Model for holding information about configuration properties and assigned values.
 *
 * @author Luka Šarc
 */
public class ConfigurationProperty {

    private final String executorName;
    private final CircuitBreakerConfigurationType type;
    private final String typeKey;
    private final String property;

    private Object value;
    private TimeUnit timeUnit;

    public ConfigurationProperty(CircuitBreakerConfigurationType type, String typeKey, String property) {
        this.executorName = null;
        this.type = type;
        this.typeKey = typeKey;
        this.property = property;
    }

    public ConfigurationProperty(String executorName, CircuitBreakerConfigurationType type, String typeKey, String property) {
        this.executorName = executorName;
        this.type = type;
        this.typeKey = typeKey;
        this.property = property;
    }

    public String getExecutorName() {
        return executorName;
    }

    public CircuitBreakerConfigurationType getType() {
        return type;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public String getProperty() {
        return property;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public String configurationPath() {
        return CircuitBreakerHelper.getBaseConfigPath(type, typeKey, executorName) + "." + property;
    }

    public static ConfigurationProperty create(String keyPath) {

        List<String> keyPathSplit = new ArrayList<String>(Arrays.asList(keyPath.split("\\.")));

        if (keyPathSplit.size() > 3 && keyPathSplit.get(0).equals(CircuitBreakerUtil.SERVICE_NAME)) {

            String executorName = null;
            CircuitBreakerConfigurationType type = CircuitBreakerConfigurationType.toEnum(keyPathSplit.get(1));
            String typeKey = keyPathSplit.get(2);
            String propertyKey = keyPathSplit.get(3);

            if (keyPathSplit.size() > 4) {
                propertyKey = keyPathSplit.get(4);

                if (type == CircuitBreakerConfigurationType.COMMAND) {
                    executorName = keyPathSplit.get(3);
                } else {
                    executorName = keyPathSplit.get(1);
                    type = CircuitBreakerConfigurationType.toEnum(keyPathSplit.get(2));
                    typeKey = keyPathSplit.get(3);
                }
            }

            return new ConfigurationProperty(executorName, type, typeKey, propertyKey);
        }

        return null;
    }

}
