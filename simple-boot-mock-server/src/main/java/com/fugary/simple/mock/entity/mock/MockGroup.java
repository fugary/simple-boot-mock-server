package com.fugary.simple.mock.entity.mock;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created on 2020/5/3 22:27 .<br>
 *
 * @author gary.fu
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MockGroup extends MockBase implements HistoryBase {

    private static final long serialVersionUID = -8012815542682860804L;
    private String userName;
    private String groupName;
    private String groupPath;
    private String proxyUrl;
    private String projectCode;
    private Integer delay;
    private String contentType;
    private String description;
    private String groupConfig;
    private Boolean disableMock;
    @Version
    @TableField("data_version")
    private Integer version;
    private Integer modifyFrom;
}
