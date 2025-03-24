package org.originit.jdk8.str.uppercase;

import java.util.concurrent.ConcurrentSkipListMap;

public class CustomUpperCaseUtil {

    public static String toUpperCase(String str) {
        ConcurrentSkipListMap<String, Object> stringObjectConcurrentSkipListMap = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
        stringObjectConcurrentSkipListMap.get("hello");
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] >= 'a' && chars[i] <= 'z') {
                chars[i] = (char) (chars[i] - 32);
            }
        }
        return new String(chars);
    }
}
