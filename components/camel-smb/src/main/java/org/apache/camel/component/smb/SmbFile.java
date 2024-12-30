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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.share.File;
import org.apache.camel.Exchange;
import org.apache.camel.component.file.GenericFile;

public class SmbFile extends GenericFile<FileIdBothDirectoryInformation> {

    private final SmbOperations operations;

    public SmbFile(SmbOperations operations) {
        this.operations = operations;
    }

    void populateHeaders(Exchange exchange) {
        exchange.getMessage().setHeader(SmbConstants.SMB_FILE_PATH, this.getPath());
        exchange.getMessage().setHeader(SmbConstants.SMB_UNC_PATH, this.getUncPath());
        exchange.getMessage().setHeader(Exchange.FILE_NAME, this.getFileName());
    }

    @Override
    public FileIdBothDirectoryInformation getFile() {
        return super.getFile();
    }

    public String getPath() {
        return this.getAbsoluteFilePath();
    }

    public String getUncPath() {
        try (File f = operations.getFile(this.getAbsoluteFilePath())) {
            return f.getUncPath();
        }
    }

    public InputStream getInputStream() {
        // from body so that smb file handle can be closed
        return new ByteArrayInputStream(operations.getBody(this.getAbsoluteFilePath()));
    }

    public long getSize() {
        return this.getFileLength();
    }

    @Override
    public Object getBody() {
        return operations.getBody(this.getAbsoluteFilePath());
    }
}
