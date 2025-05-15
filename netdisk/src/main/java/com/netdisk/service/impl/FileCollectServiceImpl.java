package com.netdisk.service.impl;

import com.netdisk.entity.po.FileCollect;
import com.netdisk.entity.po.FileInfo;
import com.netdisk.entity.query.FileCollectQuery;
import com.netdisk.mappers.FileCollectMapper;
import com.netdisk.mappers.FileInfoMapper;
import com.netdisk.service.FileCollectService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class FileCollectServiceImpl implements FileCollectService {

    @Resource
    private FileCollectMapper<FileCollect, FileCollectQuery> fileCollectMapper;
    @Resource
    private FileInfoMapper<FileInfo, ?> fileInfoMapper;

    /**
     * 递归收集所有子文件/子目录fileId（含自身）
     */
    private Set<String> collectAllSubFileIds(List<String> fileIds) {
        Set<String> allIds = new HashSet<>();
        for (String fileId : fileIds) {
            FileInfo fileInfo = fileInfoMapper.selectByFileId(fileId);
            if (fileInfo != null) {
                String ownerUserId = fileInfo.getUserId();
                List<String> subIds = fileInfoMapper.selectAllSubFileIds(fileId, ownerUserId);
                if (subIds != null) allIds.addAll(subIds);
            }
        }
        return allIds;
    }

    /**
     * 批量递归收藏
     */
    @Override
    public Integer setCollectFiles(List<String> fileIds, String userId) {
        if (fileIds == null || fileIds.isEmpty()) return 0;
        Set<String> allToCollect = collectAllSubFileIds(fileIds);
        if (allToCollect.isEmpty()) return 0;
        List<String> alreadyCollected = fileCollectMapper.selectCollectedFileIds(new ArrayList<>(allToCollect), userId);
        if (alreadyCollected != null) allToCollect.removeAll(alreadyCollected);
        if (allToCollect.isEmpty()) return 0;

        List<FileCollect> collects = new ArrayList<>();
        Date now = new Date();
        for (String fileId : allToCollect) {
            FileCollect collect = new FileCollect();
            collect.setCollectId(UUID.randomUUID().toString());
            collect.setUserId(userId);
            collect.setFileId(fileId);
            collect.setCreateTime(now);
            collects.add(collect);
        }
        return fileCollectMapper.setCollectFiles(collects);
    }

    /**
     * 批量递归取消收藏
     */
    @Override
    public Integer delCollect(List<String> fileIds, String userId) {
        if (fileIds == null || fileIds.isEmpty()) return 0;
        Set<String> allToCancel = collectAllSubFileIds(fileIds);
        if (allToCancel.isEmpty()) return 0;
        return fileCollectMapper.delCollect(new ArrayList<>(allToCancel), userId);
    }
}