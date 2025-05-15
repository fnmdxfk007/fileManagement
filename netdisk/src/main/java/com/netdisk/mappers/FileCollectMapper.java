package com.netdisk.mappers;

import com.netdisk.entity.po.FileCollect;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface FileCollectMapper<T, P> extends BaseMapper<T, P> {

    /**
     * 查询用户已收藏的文件ID
     */
    List<String> selectCollectedFileIds(@Param("fileIds") List<String> fileIds, @Param("userId") String userId);

    /**
     * 批量插入收藏记录
     */
    int setCollectFiles(@Param("collects") List<FileCollect> collects);

    /**
     * 批量删除收藏记录
     */
    int delCollect(@Param("fileIds") List<String> fileIds, @Param("userId") String userId);
}