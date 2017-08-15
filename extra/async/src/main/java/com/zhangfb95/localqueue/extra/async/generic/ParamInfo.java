package com.zhangfb95.localqueue.extra.async.generic;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhangfb
 */
@Data
public class ParamInfo {

    private List<Param> params = new ArrayList<>();
    private List<Object> paramDataList = new ArrayList<>();
}
