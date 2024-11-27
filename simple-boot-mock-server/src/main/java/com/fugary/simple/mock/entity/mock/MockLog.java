package com.fugary.simple.mock.entity.mock;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Builder
@Data
public class MockLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String creator;
    private Date createDate;
    private String userName;
    private String logName;
    private String dataId;
    private String logMessage;
    private String logType;
    private String logResult;
    private String logData;
    private String headers;
    private String ipAddress;
    private Long logTime;
    private String exceptions;
    private String requestUrl;
    private String mockGroupPath;
    private String responseBody;
    private String extend1;
    private String extend2;
}
