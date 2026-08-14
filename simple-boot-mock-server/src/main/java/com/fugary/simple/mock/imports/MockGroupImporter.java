package com.fugary.simple.mock.imports;

import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.export.ExportMockVo;

import java.util.List;

/**
 * 导入工具接口，方便后续处理其他格式导入
 * Create date 2024/7/12<br>
 *
 * @author gary.fu
 */
public interface MockGroupImporter {
    /**
     * 是否支持
     *
     * @param type
     * @return
     */
    default boolean isSupport(String type) {
        return type != null && !"".equals(getType()) && getType().equalsIgnoreCase(type);
    }

    /**
     * 解析数据
     *
     * @param data
     * @return
     */
    ExportMockVo doImport(String data);

    /**
     * 获取导入器类型
     *
     * @return
     */
    default String getType() {
        return "";
    }

    /**
     * 获取导入器类型显示名称
     *
     * @return
     */
    default String getTypeName() {
        return SimpleResultUtils.getMessage("simple.mock.import.type." + getType(), null, getType());
    }

    /**
     * 获取指定类型的显示名称
     *
     * @param importers 导入器列表
     * @param type      类型
     * @return
     */
    static String getTypeName(List<MockGroupImporter> importers, String type) {
        MockGroupImporter importer = findImporter(importers, type);
        if (importer != null) {
            return importer.getTypeName();
        }
        return SimpleResultUtils.getMessage("simple.mock.import.type." + type, null, type);
    }

    /**
     * 快速特征指纹匹配，判断数据是否符合该导入器格式
     *
     * @param data 导入文本数据
     * @return 是否匹配
     */
    default boolean match(String data) {
        return false;
    }

    /**
     * 查找可用导入器
     *
     * @param importers 导入器
     * @param type      类型
     * @return
     */
    static MockGroupImporter findImporter(List<MockGroupImporter> importers, String type) {
        if (importers == null) {
            return null;
        }
        return importers.stream()
                .filter(importer -> importer.isSupport(type))
                .findFirst()
                .orElse(null);
    }

    /**
     * 自动探测数据最匹配的导入器
     *
     * @param importers 导入器列表
     * @param data      导入文本数据
     * @return 匹配到的导入器，若未匹配返回 null
     */
    static MockGroupImporter detectImporter(List<MockGroupImporter> importers, String data) {
        if (importers == null || data == null) {
            return null;
        }
        return importers.stream()
                .filter(importer -> importer.match(data))
                .findFirst()
                .orElse(null);
    }
}

