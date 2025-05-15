package com.netdisk.service;

import java.util.List;

public interface FileCollectService {
    /**
     * 批量收藏文件
     */
    Integer setCollectFiles(List<String> fileIds, String userId);

    /**
     * 批量取消收藏文件
     */
    Integer delCollect(List<String> fileIds, String userId);

}
