package com.fugary.simple.mock.entity.mock;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

/**
 * Created on 2020/5/3 22:28 .<br>
 *
 * @author gary.fu
 */
@Data
public class MockRequest extends MockBase implements HistoryBase{

    private static final long serialVersionUID = 577338138715630744L;
    private Integer groupId;
    private String requestPath;
    private String proxyUrl;
    private String method;
    private String matchPattern;
    private String requestName;
    private Integer delay;
    private String description;
    private String mockParams;
    private String loadBalancer;
    @Version
    @TableField("data_version")
    private Integer version;
    private Integer modifyFrom;
    private Boolean disableMock;
}
