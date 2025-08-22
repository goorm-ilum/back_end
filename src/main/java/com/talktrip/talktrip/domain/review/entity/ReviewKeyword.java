package com.talktrip.talktrip.domain.review.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Document(collection = "review_keywords")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewKeyword {

    @Id
    private String id;            // MongoDB 의 ObjectId(_id)를 문자열로 받기

    private int reviewId;
    private int productId;
    private String sentence;
    private String aspect;
    private int polarity;

}
