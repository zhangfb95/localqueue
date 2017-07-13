package com.zhangfb95.localqueue.util;

import java.util.Objects;

/**
 * @author zhangfb
 */
public class Strings {

    /**
     * 字符串填充后输出
     *
     * @param template 模板字符串
     * @param args     填充参数
     * @return 格式化后输出
     */
    public static String format(String template, Object... args) {
        if (Objects.isNull(args) || args.length < 1) {
            return template;
        }

        Object[] inputArgs = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (Objects.isNull(arg)) {
                inputArgs[i] = null;
            } else {
                if (String.class.isInstance(arg)) {
                    inputArgs[i] = arg;
                } else {
                    inputArgs[i] = arg.toString();
                }
            }
        }
        return String.format(template, inputArgs);
    }
}
