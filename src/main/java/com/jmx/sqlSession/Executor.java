package com.jmx.sqlSession;

/**
 * Executor接口，定义CRUD语句，此处只增加查询的功能
 */
public interface Executor {
    <T>T query(String statement, Object parameter);
}
