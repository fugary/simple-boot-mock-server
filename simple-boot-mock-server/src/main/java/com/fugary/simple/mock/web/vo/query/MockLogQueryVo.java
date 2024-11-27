package com.fugary.simple.mock.web.vo.query;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * Create date 2024/11/14<br>
 *
 * @author gary.fu
 */
@Data
public class MockLogQueryVo extends SimpleQueryVo {

    private static final long serialVersionUID = -2076599168736875601L;
    private String userName;
    private String logName;
    private String logType;
    private String logResult;
    private String ipAddress;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endDate;
}
