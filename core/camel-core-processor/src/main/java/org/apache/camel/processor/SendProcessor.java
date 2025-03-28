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
package org.apache.camel.processor;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProducer;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.EndpointAware;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ExchangePropertyKey;
import org.apache.camel.Traceable;
import org.apache.camel.spi.HeadersMapFactory;
import org.apache.camel.spi.IdAware;
import org.apache.camel.spi.ProducerCache;
import org.apache.camel.spi.RouteIdAware;
import org.apache.camel.support.AsyncProcessorSupport;
import org.apache.camel.support.EndpointHelper;
import org.apache.camel.support.EventHelper;
import org.apache.camel.support.ExchangeHelper;
import org.apache.camel.support.cache.DefaultProducerCache;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StopWatch;
import org.apache.camel.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor for forwarding exchanges to a static endpoint destination.
 *
 * @see SendDynamicProcessor
 */
public class SendProcessor extends AsyncProcessorSupport implements Traceable, EndpointAware, IdAware, RouteIdAware {

    private static final Logger LOG = LoggerFactory.getLogger(SendProcessor.class);

    protected transient String traceLabelToString;
    protected final CamelContext camelContext;
    protected final ExchangePattern pattern;
    protected ProducerCache producerCache;
    protected AsyncProducer producer;
    protected HeadersMapFactory headersMapFactory;
    protected final Endpoint destination;
    protected String variableSend;
    protected String variableReceive;
    protected ExchangePattern destinationExchangePattern;
    protected String id;
    protected String routeId;
    protected boolean extendedStatistics;
    protected final AtomicLong counter = new AtomicLong();

    public SendProcessor(Endpoint destination) {
        this(destination, null);
    }

    public SendProcessor(Endpoint destination, ExchangePattern pattern) {
        ObjectHelper.notNull(destination, "destination");
        this.destination = destination;
        this.camelContext = destination.getCamelContext();
        ObjectHelper.notNull(this.camelContext, "camelContext");
        this.pattern = pattern;
        this.destinationExchangePattern = null;
        this.destinationExchangePattern = EndpointHelper.resolveExchangePatternFromUrl(destination.getEndpointUri());
    }

    @Override
    public String toString() {
        return destination != null ? destination.toString() : id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getRouteId() {
        return routeId;
    }

    @Override
    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    @Override
    public String getTraceLabel() {
        if (traceLabelToString == null) {
            traceLabelToString = URISupport.sanitizeUri(destination.getEndpointUri());
        }
        return traceLabelToString;
    }

    @Override
    public Endpoint getEndpoint() {
        return destination;
    }

    @Override
    public boolean process(Exchange exchange, final AsyncCallback callback) {
        if (!isStarted()) {
            exchange.setException(new IllegalStateException("SendProcessor has not been started: " + this));
            callback.done(true);
            return true;
        }

        // we should preserve existing MEP so remember old MEP
        // if you want to permanently to change the MEP then use .setExchangePattern in the DSL
        final ExchangePattern existingPattern = exchange.getPattern();

        // when using variables then we need to remember original data
        Object body = null;
        Map<String, Object> headers = null;
        if (variableSend != null || variableReceive != null) {
            try {
                body = exchange.getMessage().getBody();
                // do a defensive copy of the headers
                headers = headersMapFactory.newMap(exchange.getMessage().getHeaders());
            } catch (Exception throwable) {
                exchange.setException(throwable);
                callback.done(true);
                return true;
            }
        }
        final Object originalBody = body;
        final Map<String, Object> originalHeaders = headers;

        if (extendedStatistics) {
            counter.incrementAndGet();
        }

        // if we have a producer then use that as its optimized
        if (producer != null) {
            return sendUsingProducer(exchange, callback, existingPattern, originalBody, originalHeaders);
        } else {
            // we can send with a different MEP pattern
            return sendUsingPattern(exchange, callback, existingPattern, originalBody, originalHeaders);
        }
    }

    private boolean sendUsingPattern(
            Exchange exchange, AsyncCallback callback, ExchangePattern existingPattern, Object originalBody,
            Map<String, Object> originalHeaders) {
        if (destinationExchangePattern != null || pattern != null) {
            exchange.setPattern(destinationExchangePattern != null ? destinationExchangePattern : pattern);
        }
        // set property which endpoint we send to
        exchange.setProperty(ExchangePropertyKey.TO_ENDPOINT, destination.getEndpointUri());

        // replace message body with variable
        if (variableSend != null) {
            Object value = ExchangeHelper.getVariable(exchange, variableSend);
            exchange.getMessage().setBody(value);
        }

        LOG.debug(">>>> {} {}", destination, exchange);

        // send the exchange to the destination using the producer cache for the non optimized producers
        return producerCache.doInAsyncProducer(destination, exchange, callback,
                (producer, ex, cb) -> producer.process(ex, doneSync -> {
                    // restore previous MEP
                    exchange.setPattern(existingPattern);
                    // result should be stored in variable instead of message body/headers
                    if (ExchangeHelper.shouldSetVariableResult(exchange, variableReceive)) {
                        ExchangeHelper.setVariableFromMessageBodyAndHeaders(exchange, variableReceive,
                                exchange.getMessage());
                        exchange.getMessage().setBody(originalBody);
                        exchange.getMessage().setHeaders(originalHeaders);
                    }
                    // signal we are done
                    cb.done(doneSync);
                }));
    }

    private boolean sendUsingProducer(
            Exchange exchange, AsyncCallback callback, ExchangePattern existingPattern, Object originalBody,
            Map<String, Object> originalHeaders) {
        final Exchange target = exchange;
        // we can send with a different MEP pattern
        if (destinationExchangePattern != null || pattern != null) {
            target.setPattern(destinationExchangePattern != null ? destinationExchangePattern : pattern);
        }
        // set property which endpoint we send to
        exchange.setProperty(ExchangePropertyKey.TO_ENDPOINT, destination.getEndpointUri());

        final boolean sending = camelContext.getCamelContextExtension().isEventNotificationApplicable()
                && EventHelper.notifyExchangeSending(exchange.getContext(), target, destination);
        // record timing for sending the exchange using the producer
        StopWatch watch;
        if (sending) {
            watch = new StopWatch();
        } else {
            watch = null;
        }

        // optimize to only create a new callback if really needed, otherwise we can use the provided callback as-is
        AsyncCallback ac = callback;
        boolean newCallback = watch != null || existingPattern != target.getPattern() || variableReceive != null;
        if (newCallback) {
            ac = doneSync -> {
                try {
                    // result should be stored in variable instead of message body/headers
                    if (ExchangeHelper.shouldSetVariableResult(target, variableReceive)) {
                        ExchangeHelper.setVariableFromMessageBodyAndHeaders(target, variableReceive,
                                target.getMessage());
                        target.getMessage().setBody(originalBody);
                        target.getMessage().setHeaders(originalHeaders);
                    }
                    // restore previous MEP
                    target.setPattern(existingPattern);
                    // emit event that the exchange was sent to the endpoint
                    if (watch != null) {
                        long timeTaken = watch.taken();
                        EventHelper.notifyExchangeSent(target.getContext(), target, destination, timeTaken);
                    }
                } finally {
                    callback.done(doneSync);
                }
            };
        }
        try {
            // replace message body with variable
            if (variableSend != null) {
                Object value = ExchangeHelper.getVariable(exchange, variableSend);
                exchange.getMessage().setBody(value);
            }

            LOG.debug(">>>> {} {}", destination, exchange);
            boolean sync = producer.process(exchange, ac);
            if (!sync) {
                EventHelper.notifyExchangeAsyncProcessingStartedEvent(camelContext, exchange);
            }
            return sync;
        } catch (Exception throwable) {
            exchange.setException(throwable);
            callback.done(true);
        }

        return true;
    }

    public String getVariableSend() {
        return variableSend;
    }

    public void setVariableSend(String variableSend) {
        this.variableSend = variableSend;
    }

    public String getVariableReceive() {
        return variableReceive;
    }

    public void setVariableReceive(String variableReceive) {
        this.variableReceive = variableReceive;
    }

    public Endpoint getDestination() {
        return destination;
    }

    public ExchangePattern getPattern() {
        return pattern;
    }

    public long getCounter() {
        return counter.get();
    }

    public void reset() {
        counter.set(0);
    }

    @Override
    protected void doInit() throws Exception {
        // only if JMX is enabled
        if (camelContext.getManagementStrategy() != null && camelContext.getManagementStrategy().getManagementAgent() != null) {
            this.extendedStatistics
                    = camelContext.getManagementStrategy().getManagementAgent().getStatisticsLevel().isExtended();
        } else {
            this.extendedStatistics = false;
        }

        // if the producer is not singleton we need to use a producer cache
        if (!destination.isSingletonProducer() && producerCache == null) {
            // use a single producer cache as we need to only hold reference for one destination
            producerCache = new DefaultProducerCache(this, camelContext, 0);
            // do not add as service as we do not want to manage the producer cache
        }

        headersMapFactory = camelContext.getCamelContextExtension().getHeadersMapFactory();
    }

    @Override
    protected void doStart() throws Exception {
        // warm up the producer by starting it so we can fail fast if there was a problem
        // however must start endpoint first
        ServiceHelper.startService(destination);

        // yes we can optimize and use the producer directly for sending
        if (destination.isSingletonProducer()) {
            this.producer = destination.createAsyncProducer();
            if (this.producer instanceof RouteIdAware ria) {
                ria.setRouteId(getRouteId());
            }
            // ensure the producer is managed and started
            camelContext.addService(this.producer, true, true);
        } else {
            // no we need the producer cache for pooled non-singleton producers
            ServiceHelper.startService(producerCache);
        }
    }

    @Override
    protected void doStop() throws Exception {
        // ensure the producer is removed before its stopped
        if (this.producer != null) {
            camelContext.removeService(this.producer);
        }
        ServiceHelper.stopService(producerCache, producer);
    }

    @Override
    protected void doShutdown() throws Exception {
        ServiceHelper.stopAndShutdownServices(producerCache, producer);
    }
}
