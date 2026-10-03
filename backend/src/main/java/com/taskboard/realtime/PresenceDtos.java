package com.taskboard.realtime;

import java.util.List;

/** 프로젝트 화면 접속자 현황. 같은 사용자가 여러 탭으로 들어와도 한 명으로 집계 */
public class PresenceDtos {

    public record PresenceUser(Long userId, String username, boolean guest) {
    }

    public record PresenceResponse(List<PresenceUser> users) {
    }
}
