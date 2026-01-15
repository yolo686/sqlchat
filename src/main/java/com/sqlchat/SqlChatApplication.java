package com.sqlchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SqlChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlChatApplication.class, args);
    }

}
