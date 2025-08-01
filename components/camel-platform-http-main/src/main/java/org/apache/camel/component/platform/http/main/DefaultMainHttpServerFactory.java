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
package org.apache.camel.component.platform.http.main;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Service;
import org.apache.camel.component.platform.http.main.authentication.BasicAuthenticationConfigurer;
import org.apache.camel.component.platform.http.main.authentication.JWTAuthenticationConfigurer;
import org.apache.camel.main.HttpManagementServerConfigurationProperties;
import org.apache.camel.main.HttpServerConfigurationProperties;
import org.apache.camel.main.MainConstants;
import org.apache.camel.main.MainHttpServerFactory;
import org.apache.camel.spi.annotations.JdkService;
import org.apache.camel.support.TempDirHelper;
import org.apache.camel.util.ObjectHelper;

@JdkService(MainConstants.PLATFORM_HTTP_SERVER)
public class DefaultMainHttpServerFactory implements CamelContextAware, MainHttpServerFactory {

    private static final String DEFAULT_UPLOAD_DIR = "${java.io.tmpdir}/camel/camel-tmp-#uuid#/";

    private CamelContext camelContext;

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public Service newHttpServer(CamelContext camelContext, HttpServerConfigurationProperties configuration) {
        MainHttpServer server = new MainHttpServer();

        server.setCamelContext(camelContext);
        server.setHost(configuration.getHost());
        server.setPort(configuration.getPort());
        server.setPath(configuration.getPath());
        if (configuration.getMaxBodySize() != null) {
            server.setMaxBodySize(configuration.getMaxBodySize());
        }
        server.setUseGlobalSslContextParameters(configuration.isUseGlobalSslContextParameters());
        server.setFileUploadEnabled(configuration.isFileUploadEnabled());
        if (configuration.isFileUploadEnabled()) {
            String dir = configuration.getFileUploadDirectory();
            if (dir == null) {
                dir = DEFAULT_UPLOAD_DIR;
            }
            dir = TempDirHelper.resolveTempDir(camelContext, null, dir);
            server.setFileUploadDirectory(dir);
        }
        server.setStaticEnabled(configuration.isStaticEnabled());
        server.setStaticContextPath(configuration.getStaticContextPath());
        server.setStaticSourceDir(configuration.getStaticSourceDir());

        if (configuration.isAuthenticationEnabled()) {
            configureAuthentication(server, configuration);
        }

        return server;
    }

    @Override
    public Service newHttpManagementServer(
            CamelContext camelContext, HttpManagementServerConfigurationProperties configuration) {
        ManagementHttpServer server = new ManagementHttpServer();

        server.setCamelContext(camelContext);
        server.setHost(configuration.getHost());
        server.setPort(configuration.getPort());
        server.setPath(configuration.getPath());

        server.setUseGlobalSslContextParameters(configuration.isUseGlobalSslContextParameters());
        server.setInfoEnabled(configuration.isInfoEnabled());
        server.setDevConsoleEnabled(configuration.isDevConsoleEnabled());
        server.setHealthCheckEnabled(configuration.isHealthCheckEnabled());
        server.setHealthPath(configuration.getHealthPath());
        server.setInfoPath(configuration.getInfoPath());
        server.setJolokiaEnabled(configuration.isJolokiaEnabled());
        server.setJolokiaPath(configuration.getJolokiaPath());
        server.setMetricsEnabled(configuration.isMetricsEnabled());
        server.setUploadEnabled(configuration.isUploadEnabled());
        server.setUploadSourceDir(configuration.getUploadSourceDir());
        server.setDownloadEnabled(configuration.isDownloadEnabled());
        server.setSendEnabled(configuration.isSendEnabled());

        if (configuration.isAuthenticationEnabled()) {
            configureAuthentication(server, configuration);
        }

        return server;
    }

    private void configureAuthentication(MainHttpServer server, HttpServerConfigurationProperties configuration) {
        if (configuration.getBasicPropertiesFile() != null) {
            BasicAuthenticationConfigurer auth = new BasicAuthenticationConfigurer();
            auth.configureAuthentication(server.getConfiguration().getAuthenticationConfig(), configuration);
        } else if (configuration.getJwtKeystoreType() != null) {
            ObjectHelper.notNull(configuration.getJwtKeystorePath(), "jwtKeyStorePath");
            ObjectHelper.notNull(configuration.getJwtKeystorePassword(), "jwtKeyStorePassword");
            JWTAuthenticationConfigurer auth = new JWTAuthenticationConfigurer();
            auth.configureAuthentication(server.getConfiguration().getAuthenticationConfig(), configuration);
        }
    }

    private void configureAuthentication(
            ManagementHttpServer server, HttpManagementServerConfigurationProperties configuration) {
        if (configuration.getBasicPropertiesFile() != null) {
            BasicAuthenticationConfigurer auth = new BasicAuthenticationConfigurer();
            auth.configureAuthentication(server.getConfiguration().getAuthenticationConfig(), configuration);
        } else if (configuration.getJwtKeystoreType() != null) {
            ObjectHelper.notNull(configuration.getJwtKeystorePath(), "jwtKeyStorePath");
            ObjectHelper.notNull(configuration.getJwtKeystorePassword(), "jwtKeyStorePassword");
            JWTAuthenticationConfigurer auth = new JWTAuthenticationConfigurer();
            auth.configureAuthentication(server.getConfiguration().getAuthenticationConfig(), configuration);
        }
    }

}
