package com.netdisk.controller;

import com.netdisk.entity.dto.SessionWebUserDto;
import com.netdisk.entity.vo.ResponseVO;
import com.netdisk.service.FileCollectService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController("fileCollectController")
@RequestMapping("/file")
@Validated
public class FileCollectController extends CommonFileController {

    @Resource
    private FileCollectService fileCollectService;

    /**
     * 批量收藏文件
     */
    @RequestMapping("/setCollect")
    public ResponseVO setCollect(HttpSession session,
                                      @RequestParam("fileIds") String fileIds) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        List<String> fileIdList = fileIds == null ? new ArrayList<>() : Arrays.asList(fileIds.split(","));
        fileCollectService.setCollectFiles(fileIdList, webUserDto.getUserId());
        return getSuccessResponseVO(null);
    }

    /**
     * 批量取消收藏文件
     */
    @RequestMapping("/delCollect")
    public ResponseVO delCollect(HttpSession session,
                                 @RequestParam("fileIds") String fileIds) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        List<String> fileIdList = fileIds == null ? new ArrayList<>() : Arrays.asList(fileIds.split(","));
        fileCollectService.delCollect(fileIdList, webUserDto.getUserId());
        return getSuccessResponseVO(null);
    }
}