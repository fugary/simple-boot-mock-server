package com.fugary.simple.mock.service.mock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.export.ExportGroupVo;
import com.fugary.simple.mock.web.vo.query.MockGroupImportParamVo;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
    Triple<MockGroup, MockRequest, MockData> matchMockData(HttpServletRequest request, Integer defaultId);

    /**
     * 计算delay时间
     * @param group
     * @param request
     * @param mockData
     */
    Integer calcDelayTime(MockGroup group, MockRequest request, MockData mockData);

    /**
     * 延迟时间
     * @param stateTime
     * @param delayTime
     */
    void delayTime(long stateTime,Integer delayTime);

    /**
     * 加载需要导出的对象
     * @param groups
     * @return
     */
    List<ExportGroupVo> loadExportGroups(List<MockGroup> groups);

    /**
     * 导入数据
     * @param file
     * @param importVo
     * @return
     */
    SimpleResult<Integer> doImport(MultipartFile file, MockGroupImportParamVo importVo);
}
