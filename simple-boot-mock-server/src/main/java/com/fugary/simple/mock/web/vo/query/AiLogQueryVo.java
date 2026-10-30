package com.fugary.simple.mock.web.vo.query;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiLogQueryVo extends SimpleQueryVo {
    private Integer configId;
    private String model;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endDate;
}
