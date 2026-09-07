package com.taskboard.dto;

import java.util.List;

public class PresenceDtos {

    public record PresenceUser(Long userId, String username, boolean guest) {
    }

    public record PresenceResponse(List<PresenceUser> users) {
    }
}
