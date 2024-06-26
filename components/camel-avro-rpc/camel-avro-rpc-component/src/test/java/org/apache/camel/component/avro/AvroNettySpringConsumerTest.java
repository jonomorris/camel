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
package org.apache.camel.component.avro;

import org.apache.camel.CamelContext;
import org.apache.camel.avro.impl.KeyValueProtocolImpl;
import org.apache.camel.avro.test.TestReflectionImpl;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.util.IOHelper;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AvroNettySpringConsumerTest extends AvroNettyConsumerTest {

    private AbstractApplicationContext applicationContext;

    @Override
    public void doPostSetup() {
        keyValue = (KeyValueProtocolImpl) applicationContext.getBean("keyValue");
        testReflection = (TestReflectionImpl) applicationContext.getBean("testReflection");
    }

    @Override
    public void doPostTearDown() {

        IOHelper.close(applicationContext);
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        String xmlPath = "org/apache/camel/component/avro/netty-consumer/";

        applicationContext = new ClassPathXmlApplicationContext(xmlPath + "base.xml", xmlPath + getRouteType().name() + ".xml");

        return SpringCamelContext.springCamelContext(applicationContext, true);
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }
}
