package com.netdisk.entity.vo;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.netdisk.entity.enums.DateTimePatternEnum;
import com.netdisk.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

public class FileCollectVO {
    private String fileId;
    private String userId;
    private String collectId;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setcollectId(String favoriteId) {
        this.collectId = collectId;
    }

    public String getcollectId() {
        return collectId;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public String toString() {
        return "收藏ID:" + (collectId == null ? "空" : collectId) +
                "，用户ID:" + (userId == null ? "空" : userId) +
                "，文件ID:" + (fileId == null ? "空" : fileId) +
                "，创建时间:" + (createTime == null ? "空" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
    }
}
