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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import com.hierynomus.smbj.utils.SmbFiles;
import org.apache.camel.Exchange;
import org.apache.camel.component.file.FileComponent;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.apache.camel.component.file.GenericFileOperations;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.util.ObjectHelper.isNotEmpty;

public class SmbOperations implements GenericFileOperations<FileIdBothDirectoryInformation> {

    private static final Logger LOG = LoggerFactory.getLogger(SmbOperations.class);

    private final SMBClient smbClient;
    private SmbEndpoint endpoint;
    private Connection connection;
    private DiskShare share;
    private Session session;

    public SmbOperations(SmbConfig smbConfig) {
        if (smbConfig != null) {
            smbClient = new SMBClient(smbConfig);
        } else {
            smbClient = new SMBClient();
        }
    }

    @Override
    public GenericFile<FileIdBothDirectoryInformation> newGenericFile() {
        return new SmbFile(this);
    }

    @Override
    public void setEndpoint(GenericFileEndpoint<FileIdBothDirectoryInformation> endpoint) {
        this.endpoint = (SmbEndpoint) endpoint;
    }

    @Override
    public boolean deleteFile(String name) throws GenericFileOperationFailedException {
        connect();
        if (share.fileExists(name)) {
            try (File f = share.openFile(name, EnumSet.of(AccessMask.GENERIC_ALL), null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OPEN, null)) {

                f.deleteOnClose();
            }
        }
        return true;
    }

    @Override
    public boolean existsFile(String name) throws GenericFileOperationFailedException {
        connect();
        return share.fileExists(name);
    }

    @Override
    public boolean renameFile(String from, String to) throws GenericFileOperationFailedException {
        connect();
        try (File src
                = share.openFile(from, EnumSet.of(AccessMask.GENERIC_ALL), null,
                        SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null)) {

            try (File dst
                    = share.openFile(to, EnumSet.of(AccessMask.GENERIC_WRITE), EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                            SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_CREATE,
                            EnumSet.of(SMB2CreateOptions.FILE_DIRECTORY_FILE))) {

                src.remoteCopyTo(dst);
            } catch (Exception e) {
                throw new GenericFileOperationFailedException(e.getMessage(), e);
            }
            src.deleteOnClose();
        }
        return true;
    }

    @Override
    public boolean buildDirectory(String directory, boolean absolute) throws GenericFileOperationFailedException {
        connect();
        SmbFiles files = new SmbFiles();
        files.mkdirs(share, directory);
        return true;
    }

    @Override
    public boolean retrieveFile(String name, Exchange exchange, long size) throws GenericFileOperationFailedException {
        if (isNotEmpty(endpoint.getLocalWorkDirectory())) {
            // local work directory is configured so we should store file
            // content as files in this local directory
            return retrieveFileToFileInLocalWorkDirectory(name, exchange);
        } else {
            // store file content directory as stream on the body
            return retrieveFileToStreamInBody(name, exchange);
        }
    }

    private boolean retrieveFileToStreamInBody(String name, Exchange exchange) throws GenericFileOperationFailedException {
        SmbFile target = (SmbFile) exchange.getProperty(FileComponent.FILE_EXCHANGE_FILE);
        ObjectHelper.notNull(target, "Exchange should have the " + FileComponent.FILE_EXCHANGE_FILE + " set");

        connect();

        // read the entire file into memory in the byte array
        try (File shareFile = share.openFile(name, EnumSet.of(AccessMask.GENERIC_READ), null,
                SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null)) {
            try (InputStream is = shareFile.getInputStream()) {
                byte[] body = is.readAllBytes();
                target.setBody(body);
            } catch (IOException e) {
                throw new GenericFileOperationFailedException(e.getMessage(), e);
            }
        }
        return true;
    }

    private boolean retrieveFileToFileInLocalWorkDirectory(String name, Exchange exchange)
            throws GenericFileOperationFailedException {
        java.io.File temp;
        java.io.File local = new java.io.File(endpoint.getLocalWorkDirectory());
        SmbFile file = (SmbFile) exchange.getProperty(FileComponent.FILE_EXCHANGE_FILE);
        ObjectHelper.notNull(file, "Exchange should have the " + FileComponent.FILE_EXCHANGE_FILE + " set");
        try {
            // use relative filename in local work directory
            String relativeName = file.getRelativeFilePath();

            temp = new java.io.File(local, relativeName + ".inprogress");

            // create directory to local work file
            local.mkdirs();
            local = new java.io.File(local, relativeName);

            // delete any existing files
            if (temp.exists()) {
                if (!FileUtil.deleteFile(temp)) {
                    throw new GenericFileOperationFailedException("Cannot delete existing local work file: " + temp);
                }
            }
            if (local.exists()) {
                if (!FileUtil.deleteFile(local)) {
                    throw new GenericFileOperationFailedException("Cannot delete existing local work file: " + local);
                }
            }

            // create new temp local work file
            if (!temp.createNewFile()) {
                throw new GenericFileOperationFailedException("Cannot create new local work file: " + temp);
            }

            // set header with the path to the local work file
            exchange.getIn().setHeader(SmbConstants.FILE_LOCAL_WORK_PATH, local.getPath());
        } catch (Exception e) {
            throw new GenericFileOperationFailedException("Cannot create new local work file: " + local, e);
        }
        try {
            file.setBody(local);
            try (File shareFile = share.openFile(name, EnumSet.of(AccessMask.GENERIC_READ), null,
                    SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null)) {

                try (InputStream is = shareFile.getInputStream()) {
                    // store content as a file in the local work directory in the temp handle
                    java.nio.file.Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {

            LOG.trace("Error occurred during retrieving file: {} to local directory. Deleting local work file: {}", name, temp);
            // failed to retrieve the file so we need to close streams and delete in progress file
            boolean deleted = FileUtil.deleteFile(temp);
            if (!deleted) {
                LOG.warn("Error occurred during retrieving file: {} to local directory. Cannot delete local work file: {}",
                        name, temp);
            }
            disconnect();
            throw new GenericFileOperationFailedException("Cannot retrieve file: " + name, e);
        }

        // operation went okay so rename temp to local after we have retrieved the data
        LOG.trace("Renaming local in progress file from: {} to: {}", temp, local);
        try {
            if (!FileUtil.renameFile(temp, local, false)) {
                throw new GenericFileOperationFailedException("Cannot rename local work file from: " + temp + " to: " + local);
            }
        } catch (IOException e) {
            throw new GenericFileOperationFailedException("Cannot rename local work file from: " + temp + " to: " + local, e);
        }

        return true;
    }

    @Override
    public void releaseRetrievedFileResources(Exchange exchange) throws GenericFileOperationFailedException {
        // no-op
    }

    @Override
    public boolean storeFile(String name, Exchange exchange, long size) throws GenericFileOperationFailedException {
        // no-op
        return false;
    }

    @Override
    public String getCurrentDirectory() throws GenericFileOperationFailedException {
        // no-op
        return "";
    }

    @Override
    public void changeCurrentDirectory(String path) throws GenericFileOperationFailedException {
        // no-op
    }

    @Override
    public void changeToParentDirectory() throws GenericFileOperationFailedException {
        // no-op
    }

    @Override
    public FileIdBothDirectoryInformation[] listFiles() throws GenericFileOperationFailedException {
        return listFiles("/");
    }

    @Override
    public FileIdBothDirectoryInformation[] listFiles(String path) throws GenericFileOperationFailedException {
        return listFiles(path, null);
    }

    public FileIdBothDirectoryInformation[] listFiles(String path, String searchPattern)
            throws GenericFileOperationFailedException {
        connect();
        return share.list(path, searchPattern).toArray(FileIdBothDirectoryInformation[]::new);
    }

    public byte[] getBody(String path) {
        connect();
        try (File shareFile = share.openFile(path, EnumSet.of(AccessMask.GENERIC_READ), null,
                SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null)) {

            try (InputStream is = shareFile.getInputStream()) {
                return is.readAllBytes();
            } catch (Exception e) {
                throw new GenericFileOperationFailedException(e.getMessage(), e);
            }
        }
    }

    // the returned file object should be closed by the caller
    public File getFile(String path) {
        connect();
        return share.openFile(path, EnumSet.of(AccessMask.GENERIC_READ), null,
                SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
    }

    private void connect() throws GenericFileOperationFailedException {
        try {
            if (connection != null && connection.isConnected()) {
                return;
            }
            connection = smbClient.connect(endpoint.getHostname(), endpoint.getPort());
            AuthenticationContext ac = new AuthenticationContext(
                    endpoint.getConfiguration().getUsername(),
                    endpoint.getConfiguration().getPassword().toCharArray(),
                    endpoint.getConfiguration().getDomain());
            session = connection.authenticate(ac);
            share = (DiskShare) session.connectShare(endpoint.getShareName());
        } catch (IOException e) {
            disconnect();
            throw new GenericFileOperationFailedException(e.getMessage(), e);
        }
    }

    public void disconnect() throws GenericFileOperationFailedException {
        IOHelper.close(connection);
        connection = null;
    }
}
