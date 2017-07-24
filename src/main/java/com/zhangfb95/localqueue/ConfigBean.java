package com.zhangfb95.localqueue;

import lombok.Data;

/**
 * @author zhangfb
 */
@Data
public class ConfigBean {

    private String fileName;
    private String dbFileName;
    private String dataFileName;
    private boolean createIfNotExist = true;
}
