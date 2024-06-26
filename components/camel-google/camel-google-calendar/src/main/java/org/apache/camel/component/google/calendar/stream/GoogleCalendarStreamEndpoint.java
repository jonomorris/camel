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
package org.apache.camel.component.google.calendar.stream;

import java.util.Map;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.EndpointServiceLocation;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.ScheduledPollEndpoint;
import org.apache.camel.util.ObjectHelper;

/**
 * Poll for changes in a Google Calendar.
 */
@UriEndpoint(firstVersion = "2.23.0",
             scheme = "google-calendar-stream",
             title = "Google Calendar Stream",
             syntax = "google-calendar-stream:index",
             consumerOnly = true,
             category = { Category.CLOUD }, headersClass = GoogleCalendarStreamConstants.class)
public class GoogleCalendarStreamEndpoint extends ScheduledPollEndpoint implements EndpointServiceLocation {

    @UriParam
    private GoogleCalendarStreamConfiguration configuration;

    public GoogleCalendarStreamEndpoint(String uri, GoogleCalendarStreamComponent component,
                                        GoogleCalendarStreamConfiguration endpointConfiguration) {
        super(uri, component);
        this.configuration = endpointConfiguration;
    }

    @Override
    public Producer createProducer() throws Exception {
        throw new UnsupportedOperationException("The camel google calendar stream component doesn't support producer");
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        // check for incompatible configuration options
        if (configuration.isSyncFlow()) {
            if (ObjectHelper.isNotEmpty(configuration.getQuery())) {
                throw new IllegalArgumentException("'query' parameter is incompatible with sync flow.");
            }
            if (configuration.isConsiderLastUpdate()) {
                throw new IllegalArgumentException("'considerLastUpdate' is incompatible with sync flow.");
            }
        }

        final GoogleCalendarStreamConsumer consumer = new GoogleCalendarStreamConsumer(this, processor);
        configureConsumer(consumer);
        return consumer;
    }

    public Calendar getClient() {
        return ((GoogleCalendarStreamComponent) getComponent()).getClient(configuration);
    }

    public GoogleCalendarStreamConfiguration getConfiguration() {
        return configuration;
    }

    public Exchange createExchange(ExchangePattern pattern, Event event) {
        Exchange exchange = super.createExchange(pattern);
        Message message = exchange.getIn();
        message.setBody(event);
        message.setHeader(GoogleCalendarStreamConstants.EVENT_ID, event.getId());
        return exchange;
    }

    @Override
    public String getServiceUrl() {
        if (ObjectHelper.isNotEmpty(
                ObjectHelper.isNotEmpty(configuration.getCalendarId()) && ObjectHelper.isNotEmpty(configuration.getUser()))) {
            return getServiceProtocol() + ":" + configuration.getUser() + ":" + configuration.getCalendarId();
        }
        return null;
    }

    @Override
    public String getServiceProtocol() {
        return "calendar-stream";
    }

    @Override
    public Map<String, String> getServiceMetadata() {
        if (configuration.getApplicationName() != null) {
            return Map.of("applicationName", configuration.getApplicationName());
        }
        return null;
    }
}
