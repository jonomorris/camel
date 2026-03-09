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
package org.apache.camel.component.as2.api.protocol;

import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.component.as2.api.AS2Header;
import org.apache.camel.component.as2.api.AS2MimeType;
import org.apache.camel.component.as2.api.util.HttpMessageUtils;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

/**
 * Provides access to the raw HTTP body bytes of an Asynchronous MDN receipt so that signature verification can be
 * performed on the original bytes if required.
 */
public class MdnCopyRequestInterceptor implements HttpRequestInterceptor {

    public static final String CAMEL_AS2_RAW_MDN_BODY = "camel-as2.raw-mdn-body";

    @Override
    public void process(final HttpRequest request, final EntityDetails entity, final HttpContext context)
            throws IOException {

        String contentTypeStr = HttpMessageUtils.getHeaderValue(request, AS2Header.CONTENT_TYPE);
        ContentType contentType = ContentType.parse(contentTypeStr);
        if (contentType == null) {
            return;
        }
        String mimeType = contentType.getMimeType();
        if (!AS2MimeType.MULTIPART_SIGNED.equalsIgnoreCase(mimeType)
                && !AS2MimeType.MULTIPART_REPORT.equalsIgnoreCase(mimeType)) {
            return;
        }

        if (request instanceof ClassicHttpRequest httpRequest && httpRequest.getEntity() != null) {
            try (InputStream is = httpRequest.getEntity().getContent()) {
                // the Message Disposition Notification (MDN) is typically a very small message just confirming
                // the receipt of an AS2 file
                byte[] bytes = is.readAllBytes();
                context.setAttribute(CAMEL_AS2_RAW_MDN_BODY, bytes);
            }
        }
    }
}
