package com.jd.bean;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginEvent {

    private Long userId;

    private String ip;

    private String loginState;

    private Long timestamp;

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("LoginEvent{");
        sb.append("userId=").append(userId);
        sb.append(", ip='").append(ip).append('\'');
        sb.append(", loginState='").append(loginState).append('\'');
        sb.append(", timestamp=").append(timestamp);
        sb.append('}');
        return sb.toString();
    }
}
