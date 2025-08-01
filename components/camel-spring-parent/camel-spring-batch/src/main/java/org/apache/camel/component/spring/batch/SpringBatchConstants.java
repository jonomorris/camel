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
package org.apache.camel.component.spring.batch;

import org.apache.camel.spi.Metadata;

/**
 * Constants.
 */
public final class SpringBatchConstants {

    public static final String DEFAULT_JOB_LAUNCHER_REF_NAME = "jobLauncher";

    @Metadata(description = "The name of the Batch Job to use", javaType = "String")
    public static final String JOB_NAME = "CamelSpringBatchJobName";

    private SpringBatchConstants() {
    }

}
