package com.jmx.sqlSession;

import com.jmx.config.Function;
import com.jmx.config.MapperBean;
import com.jmx.config.MyConfiguration;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 我的mapper的代理类，实现动态代理
 * 完成xml方法和真实方法对应，执行查询
 * 每个代理实例都有一个关联的调用处理程序。
 * 在代理实例上调用方法时，方法调用将被编码并调度到其调用处理程序的invoke方法。
 */

public class MyMapperProxy implements InvocationHandler {

    private MyConfiguration myConfiguration;
    private MySqlSession mySqlSession;

    public MyMapperProxy(MyConfiguration myConfiguration, MySqlSession mySqlSession) {
        this.myConfiguration = myConfiguration;
        this.mySqlSession = mySqlSession;
    }

    /**
     * 当代理调用方法时，会调用invoke方法
     * @param proxy 代理对象
     * @param method 代理的方法
     * @param args 代理方法的参数
     * @return 返回结果
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MapperBean mapperBean = myConfiguration.readMapper("UserMapper.xml");
        //是否是xml文件对应的接口
        if(!method.getDeclaringClass().getName().equals(mapperBean.getInterfaceName())){
            return null;
        }
        List<Function> list = mapperBean.getList();
        if(null != list || 0 != list.size()){
            for (Function function : list) {
                //id是否和接口方法名一样
                if(method.getName().equals(function.getFuncName())){
                    return mySqlSession.selectOne(function.getSql(), String.valueOf(args[0]));
                }
            }
        }
        return null;
    }
}
