package com.jmx.test;

import com.jmx.mapper.UserMapper;
import com.jmx.pojo.User;
import com.jmx.sqlSession.MySqlSession;

/**
 * 测试mybatis的查询是否正常
 */
public class TestMybatis {
    public static void main(String[] args) {  
        MySqlSession sqlSession=new MySqlSession();
        //获取mapper
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        User user = mapper.getUserById("1");
        User user2 = mapper.getUserById("2");
        System.out.println(user);
        System.out.println(user2);
    } 
}