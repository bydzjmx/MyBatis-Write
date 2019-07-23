package com.jmx.mapper;

import com.jmx.pojo.User;

/**
 * User对应的mapper接口
 */
public interface UserMapper {
    //根据id查找User
    User getUserById(String id);
}
