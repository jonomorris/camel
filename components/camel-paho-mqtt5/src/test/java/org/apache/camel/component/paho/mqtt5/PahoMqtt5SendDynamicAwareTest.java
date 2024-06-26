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
package org.apache.camel.component.paho.mqtt5;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.spi.SendDynamicAware;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PahoMqtt5SendDynamicAwareTest extends CamelTestSupport {
    PahoMqtt5SendDynamicAware pahoMqtt5SendDynamicAware;

    public void doPostSetup() {
        this.pahoMqtt5SendDynamicAware = new PahoMqtt5SendDynamicAware();
    }

    @Test
    public void testUriParsing() throws Exception {
        this.pahoMqtt5SendDynamicAware.setScheme("paho-mqtt5");
        Exchange exchange = createExchangeWithBody("The Body");
        SendDynamicAware.DynamicAwareEntry entry
                = new SendDynamicAware.DynamicAwareEntry("paho-mqtt5:destination", "paho-mqtt5:${header.test}", null, null);
        Processor processor = this.pahoMqtt5SendDynamicAware.createPreProcessor(createExchangeWithBody("Body"), entry);
        processor.process(exchange);
        assertEquals("destination", exchange.getMessage().getHeader(PahoMqtt5Constants.CAMEL_PAHO_OVERRIDE_TOPIC));
    }

    @Test
    public void testSlashedUriParsing() throws Exception {
        this.pahoMqtt5SendDynamicAware.setScheme("paho-mqtt5");
        Exchange exchange = createExchangeWithBody("The Body");
        SendDynamicAware.DynamicAwareEntry entry
                = new SendDynamicAware.DynamicAwareEntry("paho-mqtt5://destination", "paho-mqtt5://${header.test}", null, null);
        Processor processor = this.pahoMqtt5SendDynamicAware.createPreProcessor(createExchangeWithBody("Body"), entry);
        processor.process(exchange);
        assertEquals("destination", exchange.getMessage().getHeader(PahoMqtt5Constants.CAMEL_PAHO_OVERRIDE_TOPIC));
    }
}
