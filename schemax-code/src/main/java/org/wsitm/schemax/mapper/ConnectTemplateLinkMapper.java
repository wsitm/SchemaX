package org.wsitm.schemax.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.wsitm.schemax.entity.vo.ConnectTemplateLinkVO;

import java.util.List;

@Mapper
public interface ConnectTemplateLinkMapper {
    List<ConnectTemplateLinkVO> selectByConnectId(@Param("connectId") Integer connectId);

    ConnectTemplateLinkVO selectByConnectIdAndTpId(@Param("connectId") Integer connectId, @Param("tpId") Integer tpId);

    ConnectTemplateLinkVO selectDefOrFirstByConnectId(@Param("connectId") Integer connectId);

    int deleteByConnectId(@Param("connectId") Integer connectId);

    int deleteByTpId(@Param("tpId") Integer tpId);

    int deleteByTpIds(@Param("tpIds") Integer[] tpIds);

    int insertBatch(@Param("connectId") Integer connectId,
                    @Param("tpIdList") List<Integer> tpIdList,
                    @Param("defTpId") Integer defTpId);
}
