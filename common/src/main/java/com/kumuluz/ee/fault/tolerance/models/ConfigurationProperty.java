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

import com.kumuluz.ee.fault.tolerance.enums.FaultToleranceType;
import com.kumuluz.ee.fault.tolerance.exceptions.FaultToleranceConfigException;
import com.kumuluz.ee.fault.tolerance.exceptions.FaultToleranceException;
import com.kumuluz.ee.fault.tolerance.utils.FaultToleranceHelper;
import com.kumuluz.ee.fault.tolerance.utils.FaultToleranceUtilImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Model for holding information about configuration properties and assigned values.
 *
 * @author Luka Šarc
 * @since 1.0.0
 */
public class ConfigurationProperty {

    private final String commandKey;
    private final String groupKey;
    private final FaultToleranceType type;
    private final String propertyPath;
    private final boolean global;
    private final boolean groupSpecific;

    private Object value;

    public ConfigurationProperty(FaultToleranceType type, String propertyPath) {
        this.groupKey = null;
        this.commandKey = null;
        this.type = type;
        this.propertyPath = propertyPath;
        global = true;
        groupSpecific = false;
    }

    public ConfigurationProperty(String groupKey, FaultToleranceType type, String propertyPath) {
        this.groupKey = groupKey;
        this.commandKey = null;
        this.type = type;
        this.propertyPath = propertyPath;
        global = false;
        groupSpecific = true;
    }

    public ConfigurationProperty(String commandKey, String groupKey, FaultToleranceType type, String propertyPath) {
        this.commandKey = commandKey;
        this.groupKey = groupKey;
        this.type = type;
        this.propertyPath = propertyPath;
        global = false;
        groupSpecific = false;
    }

    public String getCommandKey() {
        return commandKey;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public FaultToleranceType getType() {
        return type;
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    public boolean isGlobal() {
        return global;
    }

    public boolean isGroupSpecific() {
        return groupSpecific;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String typeConfigurationPath() {
        return String.format("%s.%s", type.getKey(), propertyPath);
    }

    public String configurationPath() {
        if (!global && !groupSpecific)
            return FaultToleranceHelper.getBaseConfigPath(commandKey, groupKey, type) + "." + propertyPath;
        else if (!global && groupSpecific)
            return FaultToleranceHelper.getBaseConfigPath(groupKey, type) + "." + propertyPath;
        else if (global && !groupSpecific)
            return FaultToleranceHelper.getBaseConfigPath(type) + "." + propertyPath;
        else
            return propertyPath;
    }

    public static ConfigurationProperty create(String keyPath) throws FaultToleranceException {

        List<String> keyPathSplit = new ArrayList<String>(Arrays.asList(keyPath.split("\\.")));

        FaultToleranceType type = null;
        String groupKey = null;
        String commandKey = null;
        List<String> propertyPathList = new ArrayList<>();

        int idx = 0;

        if (keyPathSplit.size() == 0)
            throw new FaultToleranceConfigException("Configuration key '" + keyPath + "' split length is 0.");

        if (!keyPathSplit.get(idx++).equals(FaultToleranceUtilImpl.SERVICE_NAME))
            throw new FaultToleranceConfigException("Configuration first key on path '" + keyPath +
                    "' does not match service '" + FaultToleranceUtilImpl.SERVICE_NAME + "'.");

        for (int i = 1; i < keyPathSplit.size(); i++) {
            String key = keyPathSplit.get(idx++);

            if (type == null) {
                type = FaultToleranceType.toEnum(key);

                if (type == null) {
                    switch (i) {
                        case 1:
                            groupKey = key;
                            break;
                        case 2:
                            commandKey = key;
                            break;
                        default:
                            throw new FaultToleranceConfigException("Unidentified key at index " + i +
                                    " of keypath '" + keyPath + "'.");
                    }
                }
            } else {
                propertyPathList.add(key);
            }
        }

        String propertyPath = String.join(".", propertyPathList);

        if (groupKey == null && commandKey == null)
            return new ConfigurationProperty(type, propertyPath);
        else if (commandKey == null)
            return new ConfigurationProperty(groupKey, type, propertyPath);
        else
            return new ConfigurationProperty(commandKey, groupKey, type, propertyPath);
    }

}
