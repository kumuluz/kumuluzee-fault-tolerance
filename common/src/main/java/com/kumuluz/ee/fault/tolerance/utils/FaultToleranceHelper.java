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
package com.kumuluz.ee.fault.tolerance.utils;

import com.kumuluz.ee.fault.tolerance.enums.FaultToleranceType;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

/**
 * Helper class for fault tolerance.
 *
 * @author Luka Šarc
 * @since 1.0.0
 */
public class FaultToleranceHelper {

    private static final Logger log = Logger.getLogger(FaultToleranceHelper.class.getName());

    public static String getBaseConfigPath(String commandKey, String groupKey, FaultToleranceType type) {
        return String.format("%s.%s.%s.%s", FaultToleranceUtilImpl.SERVICE_NAME, groupKey, commandKey, type.getKey());
    }

    public static String getBaseConfigPath(String groupKey, FaultToleranceType type) {
        return String.format("%s.%s.%s", FaultToleranceUtilImpl.SERVICE_NAME, groupKey, type.getKey());
    }

    public static String getBaseConfigPath(FaultToleranceType type) {
        return String.format("%s.%s", FaultToleranceUtilImpl.SERVICE_NAME, type.getKey());
    }

    public static Duration parseDuration(String str) {

        long value = parseTime(str);
        ChronoUnit unit = parseTimeUnit(str);

        return Duration.of(value, unit);
    }

    public static long parseTime(String str) {

        try {
            return Long.parseLong(str.replaceAll("\\D+", ""));
        } catch (NumberFormatException e) {
            log.warning("Parsing of value '" + str + "' to time failed.");
            return 0;
        }
    }

    public static ChronoUnit parseTimeUnit(String str) {

        String unit = str.replaceAll("\\d+", "");

        switch (unit) {
            case "m":
                return ChronoUnit.MINUTES;
            case "s":
                return ChronoUnit.SECONDS;
            case "ns":
                return ChronoUnit.NANOS;
            case "ms":
            default:
                return ChronoUnit.MILLIS;
        }
    }

    public static int parseInt(String str) {

        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            log.warning("Parsing of value '" + str + "' to integer failed.");
            return 0;
        }
    }

    public static double parseDouble(String str) {

        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            log.warning("Parsing of value '" + str + "' to double failed.");
            return 0;
        }
    }

    public static boolean parseBoolean(String str) {
        return str.toLowerCase().equals("true");
    }

    public static boolean isInt(String str) {
        return str.matches("^(-?)\\d+$");
    }

    public static boolean isDouble(String str) {
        return str.matches("^(-?)\\d+\\.\\d+$");
    }

    public static boolean isBoolean(String str) {

        str = str.toLowerCase().trim();
        return str.equals("true") || str.equals("false");
    }

    public static boolean isTime(String str) {

        String unit = str.replaceAll("\\d+", "");
        String time = str.replaceAll("\\D+", "");

        return (unit.equals("m") || unit.equals("s") || unit.equals("ms") || unit.equals("ns")) &&
                isInt(time) && str.startsWith(time) && str.endsWith(unit);
    }

    public static int toIntValue(Object value) {

        if (value != null) {
            if (value instanceof Integer)
                return (Integer) value;
            else if (value instanceof Long)
                return ((Long) value).intValue();
        }

        return -1;
    }

    public static long toLongValue(Object value) {

        if (value != null) {
            if (value instanceof Integer)
                return ((Integer) value).longValue();
            else if (value instanceof Long)
                return (Long) value;
        }

        return -1L;
    }

    public static boolean toBooleanValue(Object value) {

        return value != null && value instanceof Boolean && (Boolean) value;

    }

}
