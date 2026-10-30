package com.lgg.yunpanbackendquestionservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.lgg.yunpanbackendquestionservice.mapper")
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ComponentScan("com.lgg")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.lgg.yunpanbackendserviceclient.service"})
public class YunpanBackendQuestionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(YunpanBackendQuestionServiceApplication.class, args);
    }

}
