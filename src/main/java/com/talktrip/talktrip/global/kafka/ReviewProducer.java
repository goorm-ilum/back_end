package com.talktrip.talktrip.global.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.topic.review-raw}")
    private String reviewRawTopic;

    // 리뷰 메시지 전송
    public void sendReview(String reviewJson) {
        kafkaTemplate.send(reviewRawTopic, reviewJson);

        System.out.println("Kafka 메시지 전송 완료: " + reviewJson);
    }
}