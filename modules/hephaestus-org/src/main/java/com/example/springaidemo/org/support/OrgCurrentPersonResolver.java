package com.example.springaidemo.org.support;

import com.example.springaidemo.org.exception.OrgAccessDeniedException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class OrgCurrentPersonResolver {

    private static final Long ROBOT_PERSON_ID = 102L;

    public Long currentPersonId(HttpSession session) {
        if (session == null) {
            throw new OrgAccessDeniedException("未登录或登录已过期");
        }
        Long personId = SessionUtils.getPersonId(session);
        if (personId == null) {
            throw new OrgAccessDeniedException("当前登录人员不存在");
        }
        return personId;
    }

    public Long robotPersonId() {
        return ROBOT_PERSON_ID;
    }
}
