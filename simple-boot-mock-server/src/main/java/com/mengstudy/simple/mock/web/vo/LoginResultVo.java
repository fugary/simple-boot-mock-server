package com.mengstudy.simple.mock.web.vo;

import com.mengstudy.simple.mock.entity.mock.MockUser;
import lombok.Data;

import java.io.Serializable;

@Data
public class LoginResultVo implements Serializable {

    private static final long serialVersionUID = -1911470282692277108L;
    private MockUser account;
    private String accessToken;

}
