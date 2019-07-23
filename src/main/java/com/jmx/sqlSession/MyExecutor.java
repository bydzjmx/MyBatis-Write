package com.jmx.sqlSession;

import com.jmx.config.MyConfiguration;
import com.jmx.pojo.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 封装JDBC的操作，进行数据库的CRUD
 * 1.建立连接
 * 2.进行jdbc操作
 */
public class MyExecutor implements Executor {

    private MyConfiguration xmlConfiguration = new MyConfiguration();

    /**
     * 执行查询方法
     * @param statement 查询语句，即sql语句
     * @param parameter 参数，即？代表的参数
     * @param <T> 返回值泛型，传递什么类型，返回对应的类型
     * @return 返回T
     */
    @Override
    public <T>T query(String statement, Object parameter) {
        Connection connection = getConnection();
        ResultSet set = null;
        PreparedStatement pre = null;
        if (connection != null) {
            try {
                pre = connection.prepareStatement(statement);
                //设置参数
                pre.setString(1,parameter.toString());
                //执行查询获取结果
                set = pre.executeQuery();
                //返回User对象
                User user = new User();
                //遍历结果集
                while(set.next()){
                    user.setId(set.getString(1));
                    user.setUsername(set.getString(2));
                    user.setPassword(set.getString(3));
                }
                T t = (T) user;
                return t;
            } catch (SQLException e) {
                e.printStackTrace();
            }finally{
                //关闭相应连接
                try{
                    if(set!=null){
                        set.close();
                    }if(pre!=null){
                        pre.close();
                    }
                    connection.close();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 通过解析xml全局配置文件，建立数据库连接
     * @return Connection
     */
    private Connection getConnection() {
        try {
            return xmlConfiguration.build("config.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
