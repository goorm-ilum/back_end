package com.talktrip.talktrip.global.util;

import java.util.Random;

public class RandomNicknameGenerator {

    private static final String[] ADJECTIVES = {
            "재빠른", "용감한", "귀여운", "수줍은", "든든한", "신비한", "반짝이는", "엉뚱한", "차가운", "따뜻한",
            "상냥한", "고요한", "활기찬", "느긋한", "총명한", "귀족같은", "호기심많은", "장난기많은", "행복한", "씩씩한",
            "우아한", "엉성한", "우당탕", "반듯한", "영리한", "털털한", "신나는", "미스터리한", "섬세한", "직진하는"
    };

    private static final String[] NOUNS = {
            "고양이", "호랑이", "너구리", "사자", "토끼", "햄스터", "강아지", "여우", "곰", "수달",
            "치타", "늑대", "다람쥐", "판다", "알파카", "고래", "참새", "두루미", "부엉이", "앵무새",
            "다람쥐", "펭귄", "고슴도치", "물개", "기린", "햇살", "달빛", "별똥별", "나무늘보", "백조"
    };

    private static final Random RANDOM = new Random();

    public static String generate() {
        String adjective = ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[RANDOM.nextInt(NOUNS.length)];
        return adjective + " " + noun;
    }
}
