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
package org.apache.camel.component.bean;

import org.apache.camel.BeanScope;
import org.apache.camel.builder.RouteBuilder;

public class MethodCallBeanFunctionScopeTest extends SimpleLanguageBeanFunctionScopeTest {

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:single").choice().when().method("foo", BeanScope.Singleton).to("mock:result")
                        .otherwise().to("mock:other");

                from("direct:proto").choice().when().method("foo", BeanScope.Prototype).to("mock:result")
                        .otherwise().to("mock:other");

                from("direct:request")
                        .to("direct:sub")
                        .to("direct:sub")
                        .to("direct:sub");

                from("direct:sub").choice().when().method("foo", BeanScope.Request).to("mock:result")
                        .otherwise().to("mock:other");

            }
        };
    }
}
