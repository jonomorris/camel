/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.impl.cloud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.cloud.ServiceDefinition;
import org.apache.camel.cloud.ServiceFilter;

/**
 * @deprecated since 4.7
 */
@Deprecated(since = "4.7")
public class CombinedServiceFilter implements ServiceFilter {
    private final List<ServiceFilter> delegates;
    private final int delegatesSize;

    public CombinedServiceFilter(List<ServiceFilter> delegates) {
        this.delegates = Collections.unmodifiableList(new ArrayList<>(delegates));
        this.delegatesSize = this.delegates.size();
    }

    public List<ServiceFilter> getDelegates() {
        return this.delegates;
    }

    @Override
    public List<ServiceDefinition> apply(Exchange exchange, List<ServiceDefinition> services) {
        for (int i = 0; i < delegatesSize; i++) {
            services = delegates.get(i).apply(exchange, services);
        }

        return services;
    }

    // **********************
    // Helpers
    // **********************

    public static CombinedServiceFilter wrap(ServiceFilter... delegates) {
        return new CombinedServiceFilter(Arrays.asList(delegates));
    }
}
