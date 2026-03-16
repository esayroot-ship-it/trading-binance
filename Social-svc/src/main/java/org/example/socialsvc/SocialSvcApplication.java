package org.example.socialsvc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan(basePackages = "org.example.socialsvc.mapper")
@EnableFeignClients
public class SocialSvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialSvcApplication.class, args);
    }
}
