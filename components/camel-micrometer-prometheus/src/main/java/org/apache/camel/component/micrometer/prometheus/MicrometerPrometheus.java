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
package org.apache.camel.component.micrometer.prometheus;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.BlockingHandlerDecorator;
import org.apache.camel.CamelContext;
import org.apache.camel.StaticService;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.component.micrometer.MicrometerConstants;
import org.apache.camel.component.micrometer.MicrometerUtils;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerExchangeEventNotifier;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerExchangeEventNotifierNamingStrategyDefault;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerExchangeEventNotifierNamingStrategyLegacy;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerRouteEventNotifier;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerRouteEventNotifierNamingStrategy;
import org.apache.camel.component.micrometer.messagehistory.MicrometerMessageHistoryFactory;
import org.apache.camel.component.micrometer.messagehistory.MicrometerMessageHistoryNamingStrategy;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyFactory;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyNamingStrategy;
import org.apache.camel.component.micrometer.spi.InstrumentedThreadPoolFactory;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.main.ManagementHttpServer;
import org.apache.camel.component.platform.http.vertx.VertxPlatformHttpRouter;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.spi.CamelMetricsService;
import org.apache.camel.spi.Configurer;
import org.apache.camel.spi.ManagementStrategy;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.annotations.JdkService;
import org.apache.camel.support.SimpleEventNotifierSupport;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JdkService("micrometer-prometheus")
@Configurer
@ManagedResource(description = "Micrometer Metrics Prometheus")
public class MicrometerPrometheus extends ServiceSupport implements CamelMetricsService, StaticService {

    private static final Logger LOG = LoggerFactory.getLogger(MicrometerPrometheus.class);

    private static final String CONTENT_TYPE_004 = "text/plain; version=0.0.4; charset=utf-8";
    private static final String CONTENT_TYPE_100 = "application/openmetrics-text; version=1.0.0; charset=utf-8";

    private ManagementHttpServer server;
    private VertxPlatformHttpRouter router;
    private PlatformHttpComponent platformHttpComponent;

    private CamelContext camelContext;
    private PrometheusMeterRegistry meterRegistry;
    private final Set<MeterBinder> createdBinders = new HashSet<>();

    @Metadata(defaultValue = "default", enums = "default,legacy")
    private String namingStrategy;
    @Metadata(defaultValue = "true")
    private boolean enableRoutePolicy = true;
    @Metadata(defaultValue = "all", enums = "all,route,context")
    private String routePolicyLevel = "all";
    @Metadata(defaultValue = "false")
    private boolean enableMessageHistory;
    @Metadata(defaultValue = "true")
    private boolean enableExchangeEventNotifier = true;
    @Metadata(defaultValue = "true")
    private boolean baseEndpointURIExchangeEventNotifier = true;
    @Metadata(defaultValue = "true")
    private boolean enableRouteEventNotifier = true;
    @Metadata(defaultValue = "false")
    private boolean enableInstrumentedThreadPoolFactory;
    @Metadata(defaultValue = "true")
    private boolean clearOnReload = true;
    @Metadata(defaultValue = "false")
    private boolean skipCamelInfo = false;
    @Metadata(defaultValue = "0.0.4", enums = "0.0.4,1.0.0")
    private String textFormatVersion = "0.0.4";
    @Metadata
    private String binders;
    @Metadata(defaultValue = "/observe/metrics")
    private String path = "/observe/metrics";

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    public String getNamingStrategy() {
        return namingStrategy;
    }

    /**
     * Controls the name style to use for metrics.
     * <p>
     * Default = uses micrometer naming convention. Legacy = uses the classic naming style (camelCase)
     */
    public void setNamingStrategy(String namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public boolean isEnableRoutePolicy() {
        return enableRoutePolicy;
    }

    /**
     * Set whether to enable the MicrometerRoutePolicyFactory for capturing metrics on route processing times.
     */
    public void setEnableRoutePolicy(boolean enableRoutePolicy) {
        this.enableRoutePolicy = enableRoutePolicy;
    }

    public String getRoutePolicyLevel() {
        return routePolicyLevel;
    }

    /**
     * Sets the level of information to capture. all = both context and routes.
     */
    public void setRoutePolicyLevel(String routePolicyLevel) {
        this.routePolicyLevel = routePolicyLevel;
    }

    public boolean isEnableMessageHistory() {
        return enableMessageHistory;
    }

    /**
     * Set whether to enable the MicrometerMessageHistoryFactory for capturing metrics on individual route node
     * processing times.
     * <p>
     * Depending on the number of configured route nodes, there is the potential to create a large volume of metrics.
     * Therefore, this option is disabled by default.
     */
    public void setEnableMessageHistory(boolean enableMessageHistory) {
        this.enableMessageHistory = enableMessageHistory;
    }

    public boolean isEnableExchangeEventNotifier() {
        return enableExchangeEventNotifier;
    }

    /**
     * Set whether to enable the MicrometerExchangeEventNotifier for capturing metrics on exchange processing times.
     */
    public void setEnableExchangeEventNotifier(boolean enableExchangeEventNotifier) {
        this.enableExchangeEventNotifier = enableExchangeEventNotifier;
    }

    public boolean isBaseEndpointURIExchangeEventNotifier() {
        return baseEndpointURIExchangeEventNotifier;
    }

    /**
     * Set whether to use base endpoint URI when capturing metrics on exchange processing times.
     */
    public void setBaseEndpointURIExchangeEventNotifier(boolean baseEndpointURIExchangeEventNotifier) {
        this.baseEndpointURIExchangeEventNotifier = baseEndpointURIExchangeEventNotifier;
    }

    public boolean isEnableRouteEventNotifier() {
        return enableRouteEventNotifier;
    }

    /**
     * Set whether to enable the MicrometerRouteEventNotifier for capturing metrics on the total number of routes and
     * total number of routes running.
     */
    public void setEnableRouteEventNotifier(boolean enableRouteEventNotifier) {
        this.enableRouteEventNotifier = enableRouteEventNotifier;
    }

    public boolean isEnableInstrumentedThreadPoolFactory() {
        return enableInstrumentedThreadPoolFactory;
    }

    /**
     * Set whether to gather performance information about Camel Thread Pools by injecting an
     * InstrumentedThreadPoolFactory.
     */
    public void setEnableInstrumentedThreadPoolFactory(boolean enableInstrumentedThreadPoolFactory) {
        this.enableInstrumentedThreadPoolFactory = enableInstrumentedThreadPoolFactory;
    }

    public boolean isClearOnReload() {
        return clearOnReload;
    }

    /**
     * Clear the captured metrics data when Camel is reloading routes such as when using Camel JBang.
     */
    public void setClearOnReload(boolean clearOnReload) {
        this.clearOnReload = clearOnReload;
    }

    public boolean isSkipCamelInfo() {
        return skipCamelInfo;
    }

    /**
     * Skip the evaluation of "app.info" metric which contains runtime provider information (default, `false`).
     */
    public void setSkipCamelInfo(boolean skipCamelInfo) {
        this.skipCamelInfo = skipCamelInfo;
    }

    public String getTextFormatVersion() {
        return textFormatVersion;
    }

    /**
     * The text-format version to use with Prometheus scraping.
     * <p>
     * 0.0.4 = text/plain; version=0.0.4; charset=utf-8 1.0.0 = application/openmetrics-text; version=1.0.0;
     * charset=utf-8
     */
    public void setTextFormatVersion(String textFormatVersion) {
        this.textFormatVersion = textFormatVersion;
    }

    public String getPath() {
        return path;
    }

    /**
     * The path endpoint used to expose the metrics.
     */
    public void setPath(String path) {
        this.path = path;
    }

    public String getBinders() {
        return binders;
    }

    /**
     * Additional Micrometer binders to include such as jvm-memory, processor, jvm-thread, and so forth. Multiple
     * binders can be separated by comma.
     * <p>
     * The following binders currently is available from Micrometer: class-loader, commons-object-pool2,
     * file-descriptor, hystrix-metrics-binder, jvm-compilation, jvm-gc, jvm-heap-pressure, jvm-info, jvm-memory,
     * jvm-thread, log4j2, logback, processor, uptime
     */
    public void setBinders(String binders) {
        this.binders = binders;
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();

        if (meterRegistry == null) {
            Registry camelRegistry = getCamelContext().getRegistry();
            MeterRegistry found = MicrometerUtils.getMeterRegistryFromCamelRegistry(camelRegistry,
                    MicrometerConstants.METRICS_REGISTRY_NAME);
            if (found == null) {
                found = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
                // enlist in registry so it can be reused
                camelRegistry.bind(MicrometerConstants.METRICS_REGISTRY_NAME, found);
            }
            if (!(found instanceof PrometheusMeterRegistry)) {
                throw new IllegalArgumentException(
                        "Existing MeterRegistry: " + found.getClass().getName() + " is not a PrometheusMeterRegistry type.");
            }
            meterRegistry = (PrometheusMeterRegistry) found;
        }

        if (ObjectHelper.isNotEmpty(binders)) {
            // load binders from micrometer
            initBinders();
        }

        if (isEnableRoutePolicy()) {
            MicrometerRoutePolicyFactory factory = new MicrometerRoutePolicyFactory();
            factory.setSkipCamelInfo(isSkipCamelInfo());
            if ("legacy".equalsIgnoreCase(namingStrategy)) {
                factory.setNamingStrategy(MicrometerRoutePolicyNamingStrategy.LEGACY);
            }
            if ("all".equalsIgnoreCase(routePolicyLevel)) {
                factory.getPolicyConfiguration().setContextEnabled(true);
                factory.getPolicyConfiguration().setRouteEnabled(true);
            } else if ("context".equalsIgnoreCase(routePolicyLevel)) {
                factory.getPolicyConfiguration().setContextEnabled(true);
                factory.getPolicyConfiguration().setRouteEnabled(false);
            } else {
                factory.getPolicyConfiguration().setContextEnabled(false);
                factory.getPolicyConfiguration().setRouteEnabled(true);
            }
            factory.setMeterRegistry(meterRegistry);
            // ensure factory will be started and stopped
            camelContext.addService(factory);
            camelContext.addRoutePolicyFactory(factory);
        }

        ManagementStrategy managementStrategy = camelContext.getManagementStrategy();
        if (isEnableExchangeEventNotifier()) {
            MicrometerExchangeEventNotifier notifier = new MicrometerExchangeEventNotifier();
            notifier.setSkipCamelInfo(isSkipCamelInfo());
            notifier.setBaseEndpointURI(isBaseEndpointURIExchangeEventNotifier());
            if ("legacy".equalsIgnoreCase(namingStrategy)) {
                notifier.setNamingStrategy(
                        new MicrometerExchangeEventNotifierNamingStrategyLegacy(isBaseEndpointURIExchangeEventNotifier()));
            } else {
                notifier.setNamingStrategy(
                        new MicrometerExchangeEventNotifierNamingStrategyDefault(isBaseEndpointURIExchangeEventNotifier()));
            }
            notifier.setMeterRegistry(meterRegistry);
            managementStrategy.addEventNotifier(notifier);
        }

        if (isEnableRouteEventNotifier()) {
            MicrometerRouteEventNotifier notifier = new MicrometerRouteEventNotifier();
            notifier.setSkipCamelInfo(isSkipCamelInfo());
            if ("legacy".equalsIgnoreCase(namingStrategy)) {
                notifier.setNamingStrategy(MicrometerRouteEventNotifierNamingStrategy.LEGACY);
            }
            notifier.setMeterRegistry(meterRegistry);
            managementStrategy.addEventNotifier(notifier);
        }

        if (isEnableMessageHistory()) {
            if (!camelContext.isMessageHistory()) {
                camelContext.setMessageHistory(true);
            }
            MicrometerMessageHistoryFactory factory = new MicrometerMessageHistoryFactory();
            if ("legacy".equalsIgnoreCase(namingStrategy)) {
                factory.setNamingStrategy(MicrometerMessageHistoryNamingStrategy.LEGACY);
            }
            factory.setMeterRegistry(meterRegistry);
            camelContext.setMessageHistoryFactory(factory);
        }

        if (isEnableInstrumentedThreadPoolFactory()) {
            InstrumentedThreadPoolFactory instrumentedThreadPoolFactory = new InstrumentedThreadPoolFactory(
                    meterRegistry,
                    camelContext.getExecutorServiceManager().getThreadPoolFactory());
            camelContext.getExecutorServiceManager().setThreadPoolFactory(instrumentedThreadPoolFactory);
        }

        if (clearOnReload) {
            camelContext.getManagementStrategy().addEventNotifier(new SimpleEventNotifierSupport() {

                @Override
                public boolean isEnabled(CamelEvent event) {
                    return event instanceof CamelEvent.RouteReloadedEvent;
                }

                @Override
                public void notify(CamelEvent event) throws Exception {
                    // when reloading then there may be more routes in the same batch, so we only want
                    // to log the summary at the end
                    if (event instanceof CamelEvent.RouteReloadedEvent) {
                        CamelEvent.RouteReloadedEvent re = (CamelEvent.RouteReloadedEvent) event;
                        if (re.getIndex() >= re.getTotal()) {
                            LOG.info("Resetting Micrometer Registry after reloading routes");

                            // remove all meters (not counters) that are from Camel and associated routes via routeId as tag
                            List<Meter> toRemove = new ArrayList<>();
                            for (Meter m : meterRegistry.getMeters()) {
                                String n = m.getId().getName();
                                boolean camel = n.startsWith("camel_") || n.startsWith("camel.");
                                boolean keep = n.startsWith("camel.exchanges.") || n.startsWith("camel_exchanges_");
                                // remove camel but keep those special camel.exchanges. counters
                                boolean remove = camel && !keep;
                                if (remove) {
                                    String t = m.getId().getTag("routeId");
                                    if (t != null) {
                                        toRemove.add(m);
                                    }
                                }
                            }
                            toRemove.forEach(meterRegistry::remove);
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        boolean enabled = false;
        platformHttpComponent = camelContext.getComponent("platform-http", PlatformHttpComponent.class);

        server = camelContext.hasService(ManagementHttpServer.class);
        if (server != null && server.isMetricsEnabled() && platformHttpComponent != null) {
            router = server.getRouter();
            if (router != null) {
                setupHttpScraper(router);
                LOG.info("MicrometerPrometheus enabled with HTTP scraping on management port {} on path {}",
                        server.getPort(), path);
                enabled = true;
            }
        }

        if (!enabled) {
            LOG.info("MicrometerPrometheus not enabled");
        }
    }

    private void initBinders() throws IOException {
        List<String> names = BindersHelper.discoverBinders(camelContext.getClassResolver(), binders);
        List<MeterBinder> binders = BindersHelper.loadBinders(camelContext, names);

        StringJoiner sj = new StringJoiner(", ");
        for (MeterBinder mb : binders) {
            mb.bindTo(meterRegistry);
            createdBinders.add(mb);
            sj.add(mb.getClass().getSimpleName());
        }
        if (!createdBinders.isEmpty()) {
            LOG.info("Registered {} MeterBinders: {}", createdBinders.size(), sj);
        }
    }

    @Override
    protected void doShutdown() throws Exception {
        super.doShutdown();

        for (MeterBinder mb : createdBinders) {
            if (mb instanceof Closeable ac) {
                IOHelper.close(ac);
            }
        }
        createdBinders.clear();
    }

    protected void setupHttpScraper(VertxPlatformHttpRouter router) {
        Route metrics = router.route(path);
        metrics.method(HttpMethod.GET);

        final String format
                = "0.0.4".equals(textFormatVersion) ? CONTENT_TYPE_004 : CONTENT_TYPE_100;
        metrics.produces(format);

        Handler<RoutingContext> handler = new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext ctx) {
                String ct = format;
                // the client may ask for version 1.0.0 via accept header
                String ah = ctx.request().getHeader("Accept");
                if (ah != null && ah.contains("application/openmetrics-text")) {
                    ct = CONTENT_TYPE_100;
                }

                ctx.response().putHeader("Content-Type", ct);
                String data = meterRegistry.scrape(ct);
                ctx.end(data);
            }
        };

        // use blocking handler as the task can take longer time to complete
        metrics.handler(new BlockingHandlerDecorator(handler, true));

        platformHttpComponent.addHttpManagementEndpoint(path, "GET",
                null, format, null);
    }
}
