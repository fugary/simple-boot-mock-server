package com.fugary.simple.mock.imports.fastmock;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class FastMockDto implements Serializable {
    private static final long serialVersionUID = 1585514985936481068L;
    private Integer id;
    private Integer delay;
    private String name;
    private String method;
    private Integer folderId;
    private String url;
    private String description;
    private Integer on;
    @JsonProperty("create_time")
    private Date createTime;
    @JsonProperty("update_time")
    private Date updateTime;
    @JsonProperty("create_user")
    private String createUser;
    private Integer project;
    @JsonProperty("mock_rule")
    private String mockRule;
}
