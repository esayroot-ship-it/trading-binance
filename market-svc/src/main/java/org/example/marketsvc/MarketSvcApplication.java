package org.example.marketsvc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan(basePackages = "org.example.marketsvc.mapper")
@EnableFeignClients
@EnableScheduling
public class MarketSvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketSvcApplication.class, args);
    }
}
