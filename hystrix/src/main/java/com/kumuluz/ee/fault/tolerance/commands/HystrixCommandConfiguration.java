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
package com.kumuluz.ee.fault.tolerance.commands;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixThreadPoolKey;

/**
 * Configuration for a Hystrix command.
 *
 * @author Urban Malc
 * @since 1.1.0
 */
public class HystrixCommandConfiguration {

    private HystrixCommandGroupKey groupKey;
    private HystrixCommandKey commandKey;
    private HystrixThreadPoolKey threadPoolKey;

    public HystrixCommandConfiguration(HystrixCommandGroupKey groupKey,
                                       HystrixCommandKey commandKey,
                                       HystrixThreadPoolKey threadPoolKey) {
        this.groupKey = groupKey;
        this.commandKey = commandKey;
        this.threadPoolKey = threadPoolKey;
    }

    public HystrixCommandGroupKey getGroupKey() {
        return groupKey;
    }

    public HystrixCommandKey getCommandKey() {
        return commandKey;
    }

    public HystrixThreadPoolKey getThreadPoolKey() {
        return threadPoolKey;
    }
}
