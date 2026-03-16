package org.example.messagesvc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

@SpringBootApplication
@MapperScan(basePackages = "org.example.messagesvc.mapper")
@EnableRabbit
public class MessageSvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageSvcApplication.class, args);
    }
}
