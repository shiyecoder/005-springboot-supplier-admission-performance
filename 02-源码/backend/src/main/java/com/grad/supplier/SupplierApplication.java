package com.grad.supplier;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 供应商准入与绩效评价管理系统启动类。
 * 端口 8083、库 new_005 由 application.yml 指定，切换通道时只改配置。
 */
@SpringBootApplication
@MapperScan("com.grad.supplier.mapper")
@EnableScheduling
public class SupplierApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupplierApplication.class, args);
    }
}
