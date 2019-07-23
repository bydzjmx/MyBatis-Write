package com.jmx.config;

import lombok.Data;

import java.util.List;


/**
 * 用于读取mapper，获取mapper对应的接口及statement语句
 */
@Data
public class MapperBean {
    //接口名
    private String interfaceName;
    //接口下所有方法
    private List<Function> list;
}
