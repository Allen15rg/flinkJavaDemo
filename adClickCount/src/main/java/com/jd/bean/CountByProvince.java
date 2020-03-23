package com.jd.bean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CountByProvince {

    private String windowEnd;

    private String province;

    private Long count;

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("CountByProvince{");
        sb.append("windowEnd='").append(windowEnd).append('\'');
        sb.append(", province='").append(province).append('\'');
        sb.append(", count=").append(count);
        sb.append('}');
        return sb.toString();
    }
}
