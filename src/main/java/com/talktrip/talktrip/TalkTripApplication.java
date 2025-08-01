package com.talktrip.talktrip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TalkTripApplication {

    public static void main(String[] args) {
        SpringApplication.run(TalkTripApplication.class, args);
    }

}
