package com.lagou.edu.utils;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * @author 应癫
 */
public class DruidUtils {

    private DruidUtils(){
    }

    private static DruidDataSource druidDataSource = new DruidDataSource();


    static {
        druidDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        druidDataSource.setUrl("jdbc:mysql://175.24.30.101:3306/lagouWork");
        druidDataSource.setUsername("root");
        druidDataSource.setPassword("112233");

    }

    public static DruidDataSource getInstance() {
        return druidDataSource;
    }

}
