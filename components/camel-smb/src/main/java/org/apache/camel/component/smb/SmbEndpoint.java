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
package org.apache.camel.component.smb;

import java.io.File;
import java.util.Map;

import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import org.apache.camel.Category;
import org.apache.camel.Exchange;
import org.apache.camel.FailedToCreateProducerException;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileConfiguration;
import org.apache.camel.component.file.GenericFileConsumer;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOperations;
import org.apache.camel.component.file.GenericFilePollingConsumer;
import org.apache.camel.component.file.GenericFileProcessStrategy;
import org.apache.camel.component.file.GenericFileProducer;
import org.apache.camel.component.smb.strategy.SmbProcessStrategyFactory;
import org.apache.camel.spi.EndpointServiceLocation;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receive files from SMB (Server Message Block) shares.
 */
@UriEndpoint(firstVersion = "4.3.0", scheme = "smb", title = "SMB", syntax = "smb:hostname:port/shareName",
             category = { Category.FILE })
@Metadata(excludeProperties = "appendChars,readLockIdempotentReleaseAsync,readLockIdempotentReleaseAsyncPoolSize,"
                              + "readLockIdempotentReleaseDelay,readLockIdempotentReleaseExecutorService,"
                              + "directoryMustExist,extendedAttributes,probeContentType,startingDirectoryMustExist,"
                              + "startingDirectoryMustHaveAccess,chmodDirectory,forceWrites,copyAndDeleteOnRenameFail,"
                              + "renameUsingCopy,synchronous")
public class SmbEndpoint extends GenericFileEndpoint<FileIdBothDirectoryInformation> implements EndpointServiceLocation {

    private static final Logger LOG = LoggerFactory.getLogger(SmbEndpoint.class);

    @UriPath
    @Metadata(required = true)
    private String hostname;
    @UriPath(defaultValue = "445")
    private int port;
    @UriPath
    @Metadata(required = true)
    private String shareName;

    @UriParam
    private SmbConfiguration configuration = new SmbConfiguration();

    public SmbEndpoint() {
    }

    public SmbEndpoint(String uri, SmbComponent component) {
        super(uri, component);
    }

    @Override
    public String getServiceUrl() {
        if (port != 0) {
            return hostname + ":" + port;
        } else {
            return hostname;
        }
    }

    @Override
    public String getServiceProtocol() {
        return "smb";
    }

    @Override
    public Map<String, String> getServiceMetadata() {
        if (configuration.getUsername() != null) {
            return Map.of("username", configuration.getUsername());
        }
        return null;
    }

    public GenericFileOperations<FileIdBothDirectoryInformation> createOperations() {
        SmbOperations operations = new SmbOperations(configuration.getSmbConfig());
        operations.setEndpoint(this);
        return operations;
    }

    @Override
    public String getScheme() {
        return "smb";
    }

    @Override
    public char getFileSeparator() {
        return '/';
    }

    @Override
    public boolean isAbsolute(String name) {
        return FileUtil.isAbsolute(new File(name));
    }

    @Override
    protected GenericFileProcessStrategy<FileIdBothDirectoryInformation> createGenericFileStrategy() {
        return new SmbProcessStrategyFactory().createGenericFileProcessStrategy(getCamelContext(), getParamsAsMap());
    }

    @Override
    public Exchange createExchange(GenericFile<FileIdBothDirectoryInformation> file) {
        Exchange answer = super.createExchange();
        if (file != null) {
            file.bindToExchange(answer);
        }
        return answer;
    }

    @Override
    public GenericFileProducer<FileIdBothDirectoryInformation> createProducer() throws Exception {
        try {
            return new SmbProducer(this, createOperations());
        } catch (Exception e) {
            throw new FailedToCreateProducerException(this, e);
        }
    }

    @Override
    public GenericFileConsumer<FileIdBothDirectoryInformation> createConsumer(Processor processor) {
        // if noop=true then idempotent should also be configured
        if (isNoop() && !isIdempotentSet()) {
            LOG.info("Endpoint is configured with noop=true so forcing endpoint to be idempotent as well");
            setIdempotent(true);
        }

        // if idempotent and no repository set then create a default one
        if (isIdempotentSet() && Boolean.TRUE.equals(isIdempotent()) && idempotentRepository == null) {
            LOG.info("Using default memory based idempotent repository with cache max size: {}", DEFAULT_IDEMPOTENT_CACHE_SIZE);
            idempotentRepository = MemoryIdempotentRepository.memoryIdempotentRepository(DEFAULT_IDEMPOTENT_CACHE_SIZE);
        }

        SmbConsumer consumer = new SmbConsumer(
                this, processor, createOperations(),
                processStrategy != null ? processStrategy : createGenericFileStrategy());
        consumer.setMaxMessagesPerPoll(this.getMaxMessagesPerPoll());
        consumer.setEagerLimitMaxMessagesPerPoll(this.isEagerMaxMessagesPerPoll());
        return consumer;
    }

    @Override
    public PollingConsumer createPollingConsumer() throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating GenericFilePollingConsumer with queueSize: {} blockWhenFull: {} blockTimeout: {}",
                    getPollingConsumerQueueSize(), isPollingConsumerBlockWhenFull(),
                    getPollingConsumerBlockTimeout());
        }
        GenericFilePollingConsumer result = new GenericFilePollingConsumer(this);
        result.setBlockWhenFull(isPollingConsumerBlockWhenFull());
        result.setBlockTimeout(getPollingConsumerBlockTimeout());
        return result;
    }

    @Override
    public SmbConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(GenericFileConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("SmbConfiguration expected");
        }
        this.configuration = (SmbConfiguration) configuration;
        super.setConfiguration(configuration);
    }

    public String getHostname() {
        return hostname;
    }

    /**
     * The share hostname or IP address
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    /**
     * The share port number
     */
    public void setPort(int port) {
        this.port = port;
    }

    public String getShareName() {
        return shareName;
    }

    /**
     * The name of the share to connect to.
     */
    public void setShareName(String shareName) {
        this.shareName = shareName;
    }

}
