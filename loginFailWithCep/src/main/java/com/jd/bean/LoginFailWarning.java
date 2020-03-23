package com.jd.bean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginFailWarning {

    private Long userId;

    private Long firstLoginTime;

    private Long lastLoginTime;

    private String warning;

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("LoginFailWarning{");
        sb.append("userId=").append(userId);
        sb.append(", firstLoginTime=").append(firstLoginTime);
        sb.append(", lastLoginTime=").append(lastLoginTime);
        sb.append(", warning='").append(warning).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
