/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.security.app.authenticator;

import android.os.Build;

import androidx.annotation.NonNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Provides utility methods that facilitate app signing identity verification.
 */
class AppAuthenticatorUtils {
    private AppAuthenticatorUtils() {
    }

    private static final char[] HEX_CHARACTERS = "0123456789abcdef".toCharArray();

    /**
     * Returns the API level as reported by {@code Build.VERSION.SDK_INT}.
     */
    static int getApiLevel() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * Computes the digest of the provided {@code data} using the specified {@code
     * digestAlgorithm}, returning a {@code String} representing the hex encoding of the digest.
     *
     * <p>The specified {@code digestAlgorithm} must be one supported from API level 1; use of
     * MD5 and SHA-1 are strongly discouraged.
     */
    static String computeDigest(@NonNull String digestAlgorithm, @NonNull byte[] data) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(digestAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            // Should never happen; the AppAuthenticator only accepts digest algorithms that are
            // available from API level 1.
            throw new AppAuthenticatorUtilsException(digestAlgorithm + " not supported on this "
                    + "device", e);
        }
        return toHexString(messageDigest.digest(data));
    }

    /**
     * Returns a {@code String} representing the hex encoding of the provided {@code data}.
     */
    static String toHexString(@NonNull byte[] data) {
        char[] result = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int resultIndex = i * 2;
            result[resultIndex] = HEX_CHARACTERS[(data[i] >> 4) & 0x0f];
            result[resultIndex + 1] = HEX_CHARACTERS[data[i] & 0x0f];
        }
        return new String(result);
    }

    /**
     * This {@code RuntimeException} is thrown when an unexpected error is encountered while
     * performing a utility operation.
     */
    private static class AppAuthenticatorUtilsException extends RuntimeException {
        AppAuthenticatorUtilsException(@NonNull String message, Throwable reason) {
            super(message, reason);
        }
    }
}
