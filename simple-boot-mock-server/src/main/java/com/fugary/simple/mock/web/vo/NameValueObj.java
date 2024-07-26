package com.fugary.simple.mock.web.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created on 2020/5/7 22:39 .<br>
 *
 * @author gary.fu
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class NameValueObj {

    private String name;
    private Object value;
}
