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
package org.apache.camel.management;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.apache.camel.management.DefaultManagementObjectNameStrategy.TYPE_ROUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisabledOnOs(OS.AIX)
public class ManagedRouteDumpStatsAsJSonTest extends ManagementTestSupport {

    @Test
    public void testPerformanceCounterStats() throws Exception {
        // get the stats for the route
        MBeanServer mbeanServer = getMBeanServer();
        ObjectName on = getCamelObjectName(TYPE_ROUTE, "foo");

        getMockEndpoint("mock:result").expectedMessageCount(1);

        template.asyncSendBody("direct:start", "Hello World");

        assertMockEndpointsSatisfied();

        String json = (String) mbeanServer.invoke(on, "dumpRouteStatsAsJSon", new Object[] { false, true },
                new String[] { "boolean", "boolean" });
        log.info(json);

        // should be valid json
        JsonObject jo = (JsonObject) Jsoner.deserialize(json);
        assertNotNull(jo);
        assertEquals(3, jo.getCollection("processors").size());
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start").routeId("foo")
                        .to("log:foo").id("to-log")
                        .delay(100)
                        .to("mock:result").id("to-mock");
            }
        };
    }

}
