package com.zhangfb95.localqueue;

import lombok.Data;

/**
 * @author zhangfb
 */
@Data
public class ConfigBean {

    private String fileName;
    private boolean createIfNotExist = true;
}
