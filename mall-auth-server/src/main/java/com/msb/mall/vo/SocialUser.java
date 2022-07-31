package com.msb.mall.vo;

import lombok.Data;

@Data
public class SocialUser {
   /* {
        "access_token": "2.00SiqJOGBocAMBae9d103cb3UvLrXE",
            "remind_in": "157679999",
            "expires_in": 157679999,
            "uid": "5706013282",
            "isRealName": "true"
    }*/

    private String accessToken; // token信息
    private long remindIn;
    private long expiresIn; // 过期时间
    private String uid; // uid 用户识别编号
    private boolean isRealName;

}
