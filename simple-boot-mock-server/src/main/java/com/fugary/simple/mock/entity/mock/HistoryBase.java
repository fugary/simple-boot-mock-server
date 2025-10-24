package com.fugary.simple.mock.entity.mock;

/**
 * Create date 2025/10/24<br>
 *
 * @author gary.fu
 */
public interface HistoryBase {
    /**
     * 获取ID
     *
     * @return
     */
    Integer getId();

    /**
     * 设置ID
     *
     * @param id
     */
    void setId(Integer id);
    /**
     * 获取版本号
     *
     * @return
     */
    Integer getVersion();

    /**
     * 设置版本号
     *
     * @param version
     */
    void setVersion(Integer version);

    /**
     * 获取修改源
     *
     * @return
     */
    Integer getModifyFrom();

    /**
     * 设置修改源
     *
     * @param modifyFrom
     */
    void setModifyFrom(Integer modifyFrom);
}
