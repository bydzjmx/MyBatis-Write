package com.jmx.sqlSession;

import com.jmx.config.MyConfiguration;

import java.lang.reflect.Proxy;

/**
 * 实现我的sqlSession，成员变量里有Executor和MyConfiguration
 * 通过sqlSession获取mapper接口的动态代理对象，并调用其方法执行
 */
public class MySqlSession {
    private Executor executor = new MyExecutor();
    private MyConfiguration myConfiguration = new MyConfiguration();

    public <T>T selectOne(String statement, Object parameter){
        return executor.query(statement,parameter);
    }

    /**
     * 根据相应接口声明的信息，通过动态代理生成对应的mapper实例
     * @param clas 代理的接口，为mapper接口
     * @param <T> 返回对应的对象
     * @return 具有proxy代理类的指定调用处理程序的代理实例，该代理实例由指定的类加载器定义并实现指定的接口
     */
    @SuppressWarnings("unchecked")
    public <T> T getMapper(Class<T> clas){
        //动态代理调用,,返回指定接口的代理类的实例，该接口将方法调用分派给指定的调用处理程序。
        return (T) Proxy.newProxyInstance(clas.getClassLoader(),new Class[]{clas},
                new MyMapperProxy(myConfiguration,this));
    }
}
