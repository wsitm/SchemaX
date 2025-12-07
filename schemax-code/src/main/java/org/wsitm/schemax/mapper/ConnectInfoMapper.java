package org.wsitm.schemax.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.wsitm.schemax.entity.domain.ConnectInfo;
import org.wsitm.schemax.entity.vo.ConnectInfoVO;

import java.util.List;

@Mapper
public interface ConnectInfoMapper {
    /**
     * 查询连接配置
     *
     * @param connectId 连接配置主键
     * @return 连接配置
     */
    public ConnectInfoVO selectConnectInfoByConnectId(Integer connectId);

    /**
     * 查询连接配置列表
     *
     * @param connectInfo 连接配置
     * @return 连接配置集合
     */
    public List<ConnectInfoVO> selectConnectInfoList(ConnectInfo connectInfo);

    /**
     * 新增连接配置
     *
     * @param connectInfo 连接配置
     * @return 结果
     */
    public int insertConnectInfo(ConnectInfo connectInfo);

    /**
     * 修改连接配置
     *
     * @param connectInfo 连接配置
     * @return 结果
     */
    public int updateConnectInfo(ConnectInfo connectInfo);

    /**
     * 删除连接配置
     *
     * @param connectId 连接配置主键
     * @return 结果
     */
    public int deleteConnectInfoByConnectId(Integer connectId);

    /**
     * 批量删除连接配置
     *
     * @param connectIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteConnectInfoByConnectIds(Integer[] connectIds);
}
