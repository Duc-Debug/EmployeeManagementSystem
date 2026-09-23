package com.hrm.employeemanagement.domain.task.comment;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionParserService {
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9._-]+)");

    public Set<String> extractMentionedUsernames(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> usernames = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            usernames.add(matcher.group(1));
        }
        return usernames;
    }
}

