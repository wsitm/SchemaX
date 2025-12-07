package org.wsitm.schemax.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.wsitm.schemax.entity.domain.ConnectInfo;
import org.wsitm.schemax.entity.vo.ConnectInfoVO;

import java.util.List;

@Mapper
public interface ConnectInfoMapper {

    List<ConnectInfo> findAll();

    ConnectInfo findById(String connectId);

    /**
     * 查询连接配置
     *
     * @param connectId 连接配置主键
     * @return 连接配置
     */
    ConnectInfoVO selectConnectInfoByConnectId(String connectId);

    /**
     * 查询连接配置列表
     *
     * @param connectInfo 连接配置
     * @return 连接配置集合
     */
    List<ConnectInfoVO> selectConnectInfoList(ConnectInfo connectInfo);


    int insert(ConnectInfo connectInfo);

    int update(ConnectInfo connectInfo);

    int deleteById(String connectId);

    int deleteByIds(@Param("ids") String[] ids);
}
