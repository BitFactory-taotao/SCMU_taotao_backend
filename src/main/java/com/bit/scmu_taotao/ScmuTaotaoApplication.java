package com.bit.scmu_taotao;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.bit.scmu_taotao.mapper")
@SpringBootApplication
@EnableScheduling
public class ScmuTaotaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmuTaotaoApplication.class, args);
    }

}
