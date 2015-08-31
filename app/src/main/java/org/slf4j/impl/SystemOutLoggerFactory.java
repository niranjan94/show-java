package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SystemOutLoggerFactory implements ILoggerFactory {
    static final String ANONYMOUS_TAG = "null";
    static final int TAG_MAX_LENGTH = 23;

    private final ConcurrentMap<String, Logger> loggerMap = new ConcurrentHashMap<String, Logger>();
    /**
     * Return an appropriate {@link SystemOutLoggerAdapter} instance by name.
     */
    public Logger getLogger(String name) {
        String tag = loggerNameToTag(name);
        Logger logger = loggerMap.get(tag);
        if (logger == null) {
            Logger newInstance = new SystemOutLoggerAdapter(tag);
            Logger oldInstance = loggerMap.putIfAbsent(tag, newInstance);
            logger = oldInstance == null ? newInstance : oldInstance;
        }
        return logger;
    }

    /**
     * Tag names cannot be longer than {@value #TAG_MAX_LENGTH} characters on Android platform.
     *
     * Returns the short logger tag (up to {@value #TAG_MAX_LENGTH} characters) for the given logger name.
     * Traditionally loggers are named by fully-qualified Java classes; this
     * method attempts to return a concise identifying part of such names.
     *
     * See also:
     * android/system/core/include/cutils/property.h
     * android/frameworks/base/core/jni/android_util_Log.cpp
     * dalvik.system.DalvikLogging
     *
     */
    static String loggerNameToTag(String loggerName) {
        // Anonymous logger
        if (loggerName == null) {
            return ANONYMOUS_TAG;
        }

        int length = loggerName.length();
        if (length <= TAG_MAX_LENGTH) {
            return loggerName;
        }

        int tagLength = 0;
        int lastTokenIndex = 0;
        int lastPeriodIndex;
        StringBuilder tagName = new StringBuilder(TAG_MAX_LENGTH + 3);
        while ((lastPeriodIndex = loggerName.indexOf('.', lastTokenIndex)) != -1) {
            tagName.append(loggerName.charAt(lastTokenIndex));
            // token of one character appended as is otherwise truncate it to one character
            int tokenLength = lastPeriodIndex - lastTokenIndex;
            if (tokenLength > 1) {
                tagName.append('*');
            }
            tagName.append('.');
            lastTokenIndex = lastPeriodIndex + 1;

            // check if name is already too long
            tagLength = tagName.length();
            if (tagLength > TAG_MAX_LENGTH) {
                return getSimpleName(loggerName);
            }
        }

        // Either we had no useful dot location at all
        // or last token would exceed TAG_MAX_LENGTH
        int tokenLength = length - lastTokenIndex;
        if (tagLength == 0 || (tagLength + tokenLength) > TAG_MAX_LENGTH) {
            return getSimpleName(loggerName);
        }

        // last token (usually class name) appended as is
        tagName.append(loggerName, lastTokenIndex, length);
        return tagName.toString();
    }

    private static String getSimpleName(String loggerName) {
        // Take leading part and append '*' to indicate that it was truncated
        int length = loggerName.length();
        int lastPeriodIndex = loggerName.lastIndexOf('.');
        return lastPeriodIndex != -1 && length - (lastPeriodIndex + 1) <= TAG_MAX_LENGTH
                ? loggerName.substring(lastPeriodIndex + 1)
                : '*' + loggerName.substring(length - TAG_MAX_LENGTH + 1);
    }
}
