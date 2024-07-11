package com.fugary.simple.mock.web.vo.query;

import lombok.Data;

import java.util.List;

/**
 * Create date 2024/7/10<br>
 *
 * @author gary.fu
 */
@Data
public class MockGroupQueryVo extends SimpleQueryVo {

    private static final long serialVersionUID = -5645692057415199643L;
    private String userName;
    private List<Integer> groupIds;
    private boolean exportAll;
}
