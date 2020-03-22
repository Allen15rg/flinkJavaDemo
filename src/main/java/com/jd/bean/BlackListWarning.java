package com.jd.bean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlackListWarning {

    private Long userId;

    private Long adId;

    private String msg;

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("BlackListWarning{");
        sb.append("userId=").append(userId);
        sb.append(", adId=").append(adId);
        sb.append(", msg='").append(msg).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
