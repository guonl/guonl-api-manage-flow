package com.guonl.dao;

import com.guonl.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SysConfigDao {

    @Select("SELECT config_key AS configKey, config_value AS configValue, config_desc AS configDesc, update_time AS updateTime FROM sys_config WHERE config_key = #{key}")
    SysConfig selectByKey(@Param("key") String key);

    @Select("SELECT config_key AS configKey, config_value AS configValue, config_desc AS configDesc, update_time AS updateTime FROM sys_config ORDER BY config_key")
    List<SysConfig> selectAll();

    @Update("UPDATE sys_config SET config_value = #{value} WHERE config_key = #{key}")
    int updateValue(@Param("key") String key, @Param("value") String value);

    @Update("INSERT INTO sys_config (config_key, config_value, config_desc) VALUES (#{key}, #{value}, #{desc}) " +
            "ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), config_desc = VALUES(config_desc)")
    int upsert(@Param("key") String key, @Param("value") String value, @Param("desc") String desc);
}
