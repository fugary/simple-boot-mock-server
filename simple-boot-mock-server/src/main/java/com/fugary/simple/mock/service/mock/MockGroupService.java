package com.fugary.simple.mock.service.mock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockGroup;
import org.apache.commons.lang3.tuple.Pair;

import javax.servlet.http.HttpServletRequest;

/**
 * Created on 2020/5/3 22:36 .<br>
 *
 * @author gary.fu
 */
public interface MockGroupService extends IService<MockGroup> {
    /**
     * 级联删除请求和数据
     *
     * @param id
     * @return
     */
    boolean deleteMockGroup(Integer id);

    /**
     * 检查是否有重复
     *
     * @param group
     * @return
     */
    boolean existsMockGroup(MockGroup group);

    /**
     * 匹配路径
     *
     * @param request
     * @param defaultId
     * @return
     */
    Pair<MockGroup, MockData> matchMockData(HttpServletRequest request, Integer defaultId);
}
