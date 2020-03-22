package com.jd.bean;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AdClickEvent {

    private Long userId;  //用户id

    private Long adId;  //广告id

    private String province;  // 省份

    private String city;  //城市

    private Long timeStamp;  //点击时间戳
}
