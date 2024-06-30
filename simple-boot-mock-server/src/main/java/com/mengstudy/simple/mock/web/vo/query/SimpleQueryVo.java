package com.mengstudy.simple.mock.web.vo.query;

import lombok.Data;

import java.io.Serializable;

/**
 * Created on 2020/5/4 14:22 .<br>
 *
 * @author gary.fu
 */
@Data
public class SimpleQueryVo implements Serializable {

    private static final long serialVersionUID = -4434584087743713106L;
    private String keyword;
    private SimplePage page;
}
