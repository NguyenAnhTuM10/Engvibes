package com.englishapp.sm2.dto;

/** quality: 0–5 (SM-2). Demo mapping: Again=1, Hard=3, Good=4, Easy=5 */
public record ReviewRequest(int quality) {}
