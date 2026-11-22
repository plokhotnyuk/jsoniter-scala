/*
 * Copyright (c) 2017-2026 Andriy Plokhotnyuk, and respective contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.plokhotnyuk.jsoniter_scala.core;

// A plain scan loop + a single bulk `String.getBytes` copy beats the SWAR loop only when the code is compiled by the
// Graal JIT of Oracle GraalVM (17+) or GraalVM CE (25+). C2, older GraalVM CE, and GraalVM started with
// `-XX:-UseJVMCICompiler` are faster with SWAR. Can be overridden with `-Djsoniter_scala.useScanAndBulkCopy=true|false`.
// SWAR is used when reading of system properties is not permitted by a security manager.
// The flag is a static final field to be folded by JIT compilers
final class ScanAndBulkCopy {
    static final boolean ENABLED = isEnabled();

    private ScanAndBulkCopy() {
    }

    private static boolean isEnabled() {
        try {
            String p = System.getProperty("jsoniter_scala.useScanAndBulkCopy");
            if (p != null) return Boolean.parseBoolean(p);
            String vmVersion = System.getProperty("java.vm.version"); // e.g. "25.0.5.1+0-jvmci-25.6-b27"
            String vendorVersion = System.getProperty("java.vendor.version"); // e.g. "Oracle GraalVM 25.5.5..." or "GraalVM CE 25.6.0..."
            return vmVersion != null && vmVersion.contains("-jvmci-") && vendorVersion != null &&
                    (vendorVersion.startsWith("Oracle GraalVM") ||
                            vendorVersion.startsWith("GraalVM CE") && majorVersion(vmVersion) >= 25);
        } catch (SecurityException e) {
            return false;
        }
    }

    private static int majorVersion(String v) { // "25.0.5.1+0-..." -> 25, "17.0.9+9-..." -> 17
        int len = v.length(), n = 0, i = 0;
        while (i < len) {
            int d = v.charAt(i) - '0';
            if (d < 0 || d > 9) break;
            n = n * 10 + d;
            i++;
        }
        return n;
    }
}
