package com.mengstudy.simple.mock.web.vo.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Created on 2020/5/4 14:24 .<br>
 *
 * @author gary.fu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimplePage implements Serializable {


    private static final long serialVersionUID = 3872817998806496090L;
    private long pageNumber;
    private long pageSize;
    private long totalCount;
    private long pageCount;

}
