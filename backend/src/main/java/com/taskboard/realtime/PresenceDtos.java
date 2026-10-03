package com.taskboard.realtime;

import java.util.List;

/** 프로젝트 접속자 현황 응답 */
public class PresenceDtos {

    public record PresenceUser(Long userId, String username, boolean guest) {
    }

    public record PresenceResponse(List<PresenceUser> users) {
    }
}
