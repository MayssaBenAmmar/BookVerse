package com.alibou.book.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsChatMessage {
    private String sender;
    private String content;
    private String type;
    private String timestamp;
    private String room;
    private String recipient;
}