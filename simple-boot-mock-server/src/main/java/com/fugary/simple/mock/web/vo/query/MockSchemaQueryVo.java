package com.fugary.simple.mock.web.vo.query;

import lombok.Data;

/**
 * Created on 2024/8/2 14:45 .<br>
 *
 * @author gary.fu
 */
@Data
public class MockSchemaQueryVo extends SimpleQueryVo {

    private static final long serialVersionUID = 8052026751041620724L;
    private Integer requestId;
    private Integer dataId;

}
