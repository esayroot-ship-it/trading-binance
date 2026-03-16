package org.example.tradingsvc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan(basePackages = "org.example.tradingsvc.mapper")
@EnableFeignClients
@EnableRabbit
public class TradingSvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingSvcApplication.class, args);
    }
}
