package com.guonl.dao;

import com.guonl.entity.ApiUrlDoc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ApiUrlDocDao {

    /** 按 url_id upsert（1:1） */
    int upsert(ApiUrlDoc doc);

    ApiUrlDoc selectByUrlId(@Param("urlId") Long urlId);
}
