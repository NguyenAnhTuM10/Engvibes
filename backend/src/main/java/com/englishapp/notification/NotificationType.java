package com.englishapp.notification;

public enum NotificationType {
    // Pipeline step progress
    PIPELINE_STARTED,
    PIPELINE_EXTRACTING_AUDIO,
    PIPELINE_TRANSCRIBING,
    PIPELINE_SAVING_SUBTITLES,
    PIPELINE_ENRICHING,
    PIPELINE_SUMMARIZING,
    // Terminal states
    VIDEO_PUBLISHED,
    VIDEO_FAILED,
    // User notifications
    ACHIEVEMENT_UNLOCKED,
    STREAK_UPDATED
}
