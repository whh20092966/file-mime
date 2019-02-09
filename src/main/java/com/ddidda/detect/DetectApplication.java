package com.ddidda.detect;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class DetectApplication {

    public static void main(String[] args){
        new SpringApplicationBuilder(DetectApplication.class).run(args);
    }
}
