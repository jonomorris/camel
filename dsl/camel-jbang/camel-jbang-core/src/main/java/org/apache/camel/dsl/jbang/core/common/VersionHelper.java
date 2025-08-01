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
package org.apache.camel.dsl.jbang.core.common;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import org.apache.camel.util.StringHelper;

public final class VersionHelper {

    private VersionHelper() {
    }

    /**
     * Retrieves the Spring Boot version to use, with support for overriding via system property. This is particularly
     * useful when the Camel version differs from the Camel Spring Boot version, allowing for explicit version control
     * in JBang scenarios.
     *
     * <p>
     * The method follows this precedence order:
     * <ol>
     * <li>System property {@code camel.jbang.camelSpringBootVersion} if set</li>
     * <li>Value from the provided supplier if not null</li>
     * <li>Returns null if neither option is available</li>
     * </ol>
     *
     * @param  supplier a supplier that provides the default Spring Boot version when no system property override is
     *                  present. May be null.
     * @return          the Spring Boot version string, or null if no version can be determined
     */
    public static String getSpringBootVersion(Supplier<String> supplier) {
        if (System.getProperty("camel.jbang.camelSpringBootVersion") != null) {
            return System.getProperty("camel.jbang.camelSpringBootVersion");
        } else if (supplier != null) {
            return supplier.get();
        }

        return null;
    }

    public static String getJBangVersion() {
        try {
            // find actual version in JBANG_HOME
            String homeDir = System.getenv("JBANG_HOME");
            String path = "";
            if (homeDir == null || homeDir.isBlank()) {
                // fallback to .jbang cache that has a list of latest version
                path = ".jbang/cache/";
                homeDir = CommandLineHelper.getHomeDir().toString();
            }
            Path file = Paths.get(homeDir).resolve(path + "version.txt");
            if (Files.exists(file) && Files.isRegularFile(file)) {
                String text = Files.readString(file);
                text = text.trim();
                return text;
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    public static boolean isGE(String source, String target) {
        return compare(source, target) >= 0;
    }

    public static boolean isLE(String source, String target) {
        return compare(source, target) <= 0;
    }

    public static boolean isBetween(String source, String inclusive, String exclusive) {
        return compare(source, inclusive) >= 0 && compare(source, exclusive) < 0;
    }

    public static int compare(String source, String target) {
        if (source == null || target == null || source.isBlank() || target.isBlank()) {
            return 0;
        }
        String s1 = StringHelper.before(source, ".");
        String s2 = StringHelper.after(source, ".");
        if (s1 == null) {
            s1 = StringHelper.before(source, ",");
            s2 = StringHelper.after(source, ",");
        }
        String t1 = StringHelper.before(target, ".");
        String t2 = StringHelper.after(target, ".");
        if (t1 == null) {
            t1 = StringHelper.before(target, ",");
            t2 = StringHelper.after(target, ",");
        }
        String s3 = StringHelper.after(s2, ".");
        if (s3 != null) {
            s2 = StringHelper.before(s2, ".");
        } else {
            s3 = "";
        }
        String t3 = StringHelper.after(t2, ".");
        if (t3 != null) {
            t2 = StringHelper.before(t2, ".");
        } else {
            t3 = "";
        }
        // avoid NPE
        if (s1 == null) {
            s1 = source;
        }
        if (s2 == null) {
            s2 = "";
        }
        if (t1 == null) {
            t1 = target;
        }
        if (t2 == null) {
            t2 = "";
        }
        // convert to 2-digit numbers
        if (s1.length() < 2) {
            s1 = "0" + s1;
        }
        if (s2.length() < 2) {
            s2 = "0" + s2;
        }
        if (s2.length() < 2) {
            s2 = "0" + s2;
        }
        if (s3.length() < 2) {
            s3 = "0" + s3;
        }
        if (t1.length() < 2) {
            t1 = "0" + t1;
        }
        if (t2.length() < 2) {
            t2 = "0" + t2;
        }
        if (t3.length() < 2) {
            t3 = "0" + t3;
        }

        String s = s1 + s2 + s3;
        String t = t1 + t2 + t3;
        return s.compareTo(t);
    }

    public static String extractCamelVersion() {
        return org.apache.camel.main.util.VersionHelper.extractCamelVersion();
    }

    public static String extractKameletsVersion() {
        return org.apache.camel.main.util.VersionHelper.extractKameletsVersion();
    }

    public static void setCamelVersion(String version) {
        org.apache.camel.main.util.VersionHelper.setCamelVersion(version);
    }
}
