package com.mengstudy.simple.mock.web.vo;

import com.mengstudy.simple.mock.web.vo.query.SimplePage;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Created on 2020/5/4 9:38 .<br>
 *
 * @author gary.fu
 */
@Getter
@Setter
@Builder(toBuilder = true)
public class SimpleResult<T> {

    /**
     * 响应码
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应的数据
     */
    private T data;

    /**
     * 分页数据
     */
    private SimplePage page;

}
