package com.tradev.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrustGrade {
    SEED("씨앗", "🌱", 0, 29),
    SPROUT("새싹", "🌿", 30, 59),
    FRUIT("열매", "🍎", 60, 79),
    TREE("나무", "🌳", 80, 100);

    private final String label;
    private final String icon;
    private final int minScore;
    private final int maxScore;

    public static TrustGrade fromScore(int score) {
        for (TrustGrade grade : values()) {
            if (score >= grade.minScore && score <= grade.maxScore) {
                return grade;
            }
        }
        return TREE;
    }
}
