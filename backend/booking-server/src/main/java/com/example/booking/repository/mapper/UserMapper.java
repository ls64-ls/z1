package com.example.booking.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.booking.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM \"user\" WHERE openid = #{openid}")
    User findByOpenid(@Param("openid") String openid);
}
