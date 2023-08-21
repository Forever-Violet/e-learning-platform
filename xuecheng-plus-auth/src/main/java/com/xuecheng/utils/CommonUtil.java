package com.xuecheng.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtil {

    public static boolean checkEmail(String username) {
        String rule = "^\\w+((-\\w+)|(\\.\\w+))*\\@[A-Za-z0-9]+((\\.|-)[A-Za-z0-9]+)*\\.[A-Za-z0-9]+$";
        //正则表达式的模式 编译正则表达式
        Pattern p = Pattern.compile(rule);
        //正则表达式的匹配器
        Matcher m = p.matcher(username);
        //进行正则匹配
        return m.matches();
    }

    public static boolean checkPhone(String s) {
        /*
        ^ 表示匹配字符串的开始位置。
        1 表示手机号的第一位必须是数字 1。
        [3456789] 表示手机号的第二位可以是 3、4、5、6、7、8、9 中的任意一个数字。
        \\d 表示匹配任意数字（0-9）。
        {9} 表示前面的模式（\d）连续出现 9 次，即匹配 9 个数字。
        $ 表示匹配字符串的结束位置。
        */
        String phoneRegex = "^1[3456789]\\d{9}$";  //手机号 regex: 正则表达式
        //正则表达式的模式 编译正则表达式
        Pattern p = Pattern.compile(phoneRegex);
        //正则表达式的匹配器
        Matcher m = p.matcher(s);
        //进行正则匹配
        return m.matches();
    }

    public static String getBracketsContent(String str) {
        return str.substring(str.indexOf("(") + 1, str.indexOf(")"));
    }

    public static String getRandomCode() {
        StringBuilder str = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            str.append(random.nextInt(10));
        }
        return str.toString();
    }

    public static <T> List<T> castList(Object obj, Class<T> clazz) {
        List<T> result = new ArrayList<T>();
        if (obj instanceof List<?>) {
            for (Object o : (List<?>) obj) {
                result.add(clazz.cast(o));
            }
            return result;
        }
        return result;
    }

    public static <T> Set<T> castSet(Object obj, Class<T> clazz) {
        Set<T> result = new HashSet<>();
        if (obj instanceof Set<?>) {
            for (Object o : (Set<?>) obj) {
                result.add(clazz.cast(o));
            }
            return result;
        }
        return result;
    }

}
