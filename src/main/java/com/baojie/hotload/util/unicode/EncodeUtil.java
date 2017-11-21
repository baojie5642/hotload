package com.baojie.hotload.util.unicode;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EncodeUtil {

    private static final Logger log = LoggerFactory.getLogger(EncodeUtil.class);
    private static final Pattern REG_UNICODE = Pattern.compile("[0-9A-Fa-f]{4}");

    private EncodeUtil() {

    }

    public static String unicode2String(final String unicode) {
        if (StringUtils.isBlank(unicode)) {
            log.warn("unicode string blank");
            return "";
        }
        final StringBuilder sb = new StringBuilder(64);
        final int len = unicode.length();
        for (int i = 0; i < len; i++) {
            char c1 = unicode.charAt(i);
            if (c1 == '\\' && i < len - 1) {
                char c2 = unicode.charAt(++i);
                if (c2 == 'u' && i <= len - 5) {
                    String tmp = unicode.substring(i + 1, i + 5);
                    Matcher matcher = REG_UNICODE.matcher(tmp);
                    if (matcher.find()) {
                        sb.append((char) Integer.parseInt(tmp, 16));
                        i = i + 4;
                    } else {
                        sb.append(c1).append(c2);
                    }
                } else {
                    sb.append(c1).append(c2);
                }
            } else {
                sb.append(c1);
            }
        }
        return sb.toString();
    }

    public static String string2Unicode(final String string) {
        if (StringUtils.isBlank(string)) {
            log.warn("utf-8 string blank");
            return "";
        }
        final StringBuffer unicode = new StringBuffer(64);
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            String str = Integer.toHexString(c);
            switch (4 - str.length()) {
                case 0:
                    unicode.append("\\u" + str);
                    break;
                case 1:
                    str = "0" + str;
                    unicode.append("\\u" + str);
                    break;
                case 2:
                case 3:
                default:
                    str = String.valueOf(c);
                    unicode.append(str);
                    break;
            }
        }
        return unicode.toString();
    }

}
