package com.fugary.simple.mock.entity.mock;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created on 2020/5/3 22:28 .<br>
 *
 * @author gary.fu
 */
@Data
public class MockBase implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String creator;
    private Date createDate;
    private String modifier;
    private Date modifyDate;
    private Integer status;

    public boolean isEnabled() {
        return Integer.valueOf(1).equals(status);
    }
}
