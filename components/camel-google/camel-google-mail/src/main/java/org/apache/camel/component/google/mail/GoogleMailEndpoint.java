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
package org.apache.camel.component.google.mail;

import java.util.Map;

import com.google.api.services.gmail.Gmail;
import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.google.mail.internal.GoogleMailApiCollection;
import org.apache.camel.component.google.mail.internal.GoogleMailApiName;
import org.apache.camel.component.google.mail.internal.GoogleMailConstants;
import org.apache.camel.component.google.mail.internal.GoogleMailPropertiesHelper;
import org.apache.camel.spi.EndpointServiceLocation;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.component.AbstractApiEndpoint;
import org.apache.camel.support.component.ApiMethod;
import org.apache.camel.support.component.ApiMethodPropertiesHelper;
import org.apache.camel.util.ObjectHelper;

/**
 * Manage messages in Google Mail.
 */
@UriEndpoint(firstVersion = "2.15.0",
             scheme = "google-mail",
             title = "Google Mail",
             syntax = "google-mail:apiName/methodName",
             apiSyntax = "apiName/methodName",
             consumerPrefix = "consumer",
             category = { Category.CLOUD, Category.API, Category.MAIL })
public class GoogleMailEndpoint extends AbstractApiEndpoint<GoogleMailApiName, GoogleMailConfiguration>
        implements EndpointServiceLocation {

    private Object apiProxy;

    @UriParam
    private GoogleMailConfiguration configuration;

    public GoogleMailEndpoint(String uri, GoogleMailComponent component, GoogleMailApiName apiName, String methodName,
                              GoogleMailConfiguration endpointConfiguration) {
        super(uri, component, apiName, methodName, GoogleMailApiCollection.getCollection().getHelper(apiName),
              endpointConfiguration);
        this.configuration = endpointConfiguration;
    }

    @Override
    public Producer createProducer() throws Exception {
        return new GoogleMailProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        // make sure inBody is not set for consumers
        if (inBody != null) {
            throw new IllegalArgumentException("Option inBody is not supported for consumer endpoint");
        }
        final GoogleMailConsumer consumer = new GoogleMailConsumer(this, processor);
        // also set consumer.* properties
        configureConsumer(consumer);
        return consumer;
    }

    @Override
    protected ApiMethodPropertiesHelper<GoogleMailConfiguration> getPropertiesHelper() {
        return GoogleMailPropertiesHelper.getHelper(getCamelContext());
    }

    @Override
    protected String getThreadProfileName() {
        return GoogleMailConstants.THREAD_PROFILE_NAME;
    }

    @Override
    protected void afterConfigureProperties() {
        switch (apiName) {
            case ATTACHMENTS:
                apiProxy = getClient().users().messages().attachments();
                break;
            case DRAFTS:
                apiProxy = getClient().users().drafts();
                break;
            case HISTORY:
                apiProxy = getClient().users().history();
                break;
            case LABELS:
                apiProxy = getClient().users().labels();
                break;
            case MESSAGES:
                apiProxy = getClient().users().messages();
                break;
            case THREADS:
                apiProxy = getClient().users().threads();
                break;
            case USERS:
                apiProxy = getClient().users();
                break;
            default:
                throw new IllegalArgumentException("Invalid API name " + apiName);
        }
    }

    public Gmail getClient() {
        return ((GoogleMailComponent) getComponent()).getClient(configuration);
    }

    @Override
    public Object getApiProxy(ApiMethod method, Map<String, Object> args) {
        return apiProxy;
    }

    public GoogleMailClientFactory getClientFactory() {
        return ((GoogleMailComponent) getComponent()).getClientFactory();
    }

    public void setClientFactory(GoogleMailClientFactory clientFactory) {
        ((GoogleMailComponent) getComponent()).setClientFactory(clientFactory);
    }

    @Override
    public String getServiceUrl() {
        if (ObjectHelper.isNotEmpty(ObjectHelper.isNotEmpty(configuration.getApiName())
                && ObjectHelper.isNotEmpty(configuration.getMethodName()))) {
            return getServiceProtocol() + ":" + configuration.getApiName() + ":" + configuration.getMethodName();
        }
        return null;
    }

    @Override
    public String getServiceProtocol() {
        return "mail";
    }

    @Override
    public Map<String, String> getServiceMetadata() {
        if (configuration.getApplicationName() != null) {
            return Map.of("applicationName", configuration.getApplicationName());
        }
        return null;
    }
}
