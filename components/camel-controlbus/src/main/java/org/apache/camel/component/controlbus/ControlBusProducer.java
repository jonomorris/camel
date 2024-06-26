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
package org.apache.camel.component.controlbus;

import java.util.concurrent.RejectedExecutionException;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.camel.AsyncCallback;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.apache.camel.spi.CamelLogger;
import org.apache.camel.spi.Language;
import org.apache.camel.support.DefaultAsyncProducer;
import org.apache.camel.support.ExchangeHelper;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The control bus producer.
 */
public class ControlBusProducer extends DefaultAsyncProducer {

    private static final Logger LOG = LoggerFactory.getLogger(ControlBusProducer.class);

    private final CamelLogger logger;

    public ControlBusProducer(Endpoint endpoint, CamelLogger logger) {
        super(endpoint);
        this.logger = logger;
    }

    @Override
    public ControlBusEndpoint getEndpoint() {
        return (ControlBusEndpoint) super.getEndpoint();
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        if (getEndpoint().getLanguage() != null) {
            try {
                processByLanguage(exchange, getEndpoint().getLanguage());
            } catch (Exception e) {
                exchange.setException(e);
            }
        } else if (getEndpoint().getAction() != null) {
            try {
                processByAction(exchange);
            } catch (Exception e) {
                exchange.setException(e);
            }
        }

        callback.done(true);
        return true;
    }

    protected void processByLanguage(Exchange exchange, Language language) {
        LanguageTask task = new LanguageTask(exchange, language);
        if (getEndpoint().isAsync()) {
            getEndpoint().getComponent().getExecutorService().submit(task);
        } else {
            task.run();
        }
    }

    protected void processByAction(Exchange exchange) {
        ActionTask task = new ActionTask(exchange);
        if (getEndpoint().isAsync()) {
            getEndpoint().getComponent().getExecutorService().submit(task);
        } else {
            task.run();
        }
    }

    /**
     * Tasks to run when processing by language.
     */
    private final class LanguageTask implements Runnable {

        private final Exchange exchange;
        private final Language language;

        private LanguageTask(Exchange exchange, Language language) {
            this.exchange = exchange;
            this.language = language;
        }

        @Override
        public void run() {
            String task = null;
            Object result = null;

            try {
                // create copy of exchange to not cause side effect
                Exchange copy = ExchangeHelper.createCopy(exchange, true);
                task = copy.getIn().getMandatoryBody(String.class);
                if (task != null) {
                    Expression exp = language.createExpression(task);
                    result = exp.evaluate(copy, Object.class);
                }

                if (result != null && !getEndpoint().isAsync()) {
                    // can only set result on exchange if sync
                    exchange.getIn().setBody(result);
                }

                if (task != null) {
                    logger.log("ControlBus task done [" + task + "] with result -> " + (result != null ? result : "void"));
                }
            } catch (Exception e) {
                logger.log("Error executing ControlBus task [" + task + "]. This exception will be ignored.", e);
            }
        }
    }

    /**
     * Tasks to run when processing by route action.
     */
    private final class ActionTask implements Runnable {

        private final Exchange exchange;

        private ActionTask(Exchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public void run() {
            String action = getEndpoint().getAction();
            String id = getEndpoint().getRouteId();

            if (ObjectHelper.equal("current", id)) {
                id = ExchangeHelper.getRouteId(exchange);
            }

            Object result = null;
            String task = action + " route " + id;

            try {
                if ("start".equals(action)) {
                    startAction(id);
                } else if ("stop".equals(action)) {
                    stopAction(id);
                } else if ("fail".equals(action)) {
                    failAction(id);
                } else if ("suspend".equals(action)) {
                    suspendAction(id);
                } else if ("resume".equals(action)) {
                    resumeAction(id);
                } else if ("restart".equals(action)) {
                    restartAction(id);
                } else if ("status".equals(action)) {
                    result = statusAction(id, result);
                } else if ("stats".equals(action)) {
                    result = statsAction(id);
                }

                if (result != null && !getEndpoint().isAsync()) {
                    // can only set result on exchange if sync
                    exchange.getIn().setBody(result);
                }

                logger.log("ControlBus task done [" + task + "] with result -> " + (result != null ? result : "void"));
            } catch (Exception e) {
                logger.log("Error executing ControlBus task [" + task + "]. This exception will be ignored.", e);
            }
        }

        private Object statsAction(String id)
                throws MalformedObjectNameException, InstanceNotFoundException, MBeanException, ReflectionException {
            Object result;
            LOG.debug("Route stats: {}", id);

            // camel context or per route
            String name = getEndpoint().getCamelContext().getManagementName();
            if (name == null) {
                result = "JMX is disabled, cannot get stats";
            } else {
                ObjectName on;
                String operation;
                if (id == null) {
                    CamelContext camelContext = getEndpoint().getCamelContext();
                    on = getEndpoint().getCamelContext().getManagementStrategy().getManagementObjectNameStrategy()
                            .getObjectNameForCamelContext(camelContext);
                    operation = "dumpRoutesStatsAsXml";
                } else {
                    Route route = getEndpoint().getCamelContext().getRoute(id);
                    on = getEndpoint().getCamelContext().getManagementStrategy().getManagementObjectNameStrategy()
                            .getObjectNameForRoute(route);
                    operation = "dumpRouteStatsAsXml";
                }
                if (on != null) {
                    MBeanServer server = getEndpoint().getCamelContext().getManagementStrategy().getManagementAgent()
                            .getMBeanServer();
                    result = server.invoke(on, operation, new Object[] { true, true },
                            new String[] { "boolean", "boolean" });
                } else {
                    result = "Cannot lookup route with id " + id;
                }
            }
            return result;
        }

        private Object statusAction(String id, Object result) {
            LOG.debug("Route status: {}", id);
            ServiceStatus status = getEndpoint().getCamelContext().getRouteController().getRouteStatus(id);
            if (status != null) {
                result = status.name();
            }
            return result;
        }

        private void restartAction(String id) throws Exception {
            LOG.debug("Restarting route: {}", id);
            getEndpoint().getCamelContext().getRouteController().stopRoute(id);
            int delay = getEndpoint().getRestartDelay();
            if (delay > 0) {
                try {
                    LOG.debug("Sleeping {} ms before starting route: {}", delay, id);
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    LOG.info("Interrupted while waiting before starting the route");
                    Thread.currentThread().interrupt();
                }
            }
            getEndpoint().getCamelContext().getRouteController().startRoute(id);
        }

        private void resumeAction(String id) throws Exception {
            LOG.debug("Resuming route: {}", id);
            getEndpoint().getCamelContext().getRouteController().resumeRoute(id);
        }

        private void suspendAction(String id) throws Exception {
            LOG.debug("Suspending route: {}", id);
            getEndpoint().getCamelContext().getRouteController().suspendRoute(id);
        }

        private void failAction(String id) throws Exception {
            LOG.debug("Stopping and failing route: {}", id);
            // is there any caused exception from the exchange to mark the route as failed due
            Throwable cause = exchange.getException();
            if (cause == null) {
                cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
            }
            if (cause == null) {
                cause = new RejectedExecutionException("Route " + id + " is forced stopped and marked as failed");
            }
            getEndpoint().getCamelContext().getRouteController().stopRoute(id, cause);
        }

        private void stopAction(String id) throws Exception {
            LOG.debug("Stopping route: {}", id);
            getEndpoint().getCamelContext().getRouteController().stopRoute(id);
        }

        private void startAction(String id) throws Exception {
            LOG.debug("Starting route: {}", id);
            getEndpoint().getCamelContext().getRouteController().startRoute(id);
        }

    }

}
