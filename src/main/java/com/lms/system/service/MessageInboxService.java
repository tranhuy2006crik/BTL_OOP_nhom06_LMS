package com.lms.system.service;

import org.springframework.stereotype.Service;

@Service
public class MessageInboxService {

    public InboxSummary summary() {
        return new InboxSummary(0, 0, 0);
    }

    public record InboxSummary(int starredCount, int groupCount, int privateCount) {
    }
}
