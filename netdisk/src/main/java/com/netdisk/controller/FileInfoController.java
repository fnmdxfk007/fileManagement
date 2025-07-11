package com.netdisk.controller;

import com.netdisk.annotation.GlobalInterceptor;
import com.netdisk.annotation.VerifyParam;
import com.netdisk.entity.dto.SessionWebUserDto;
import com.netdisk.entity.dto.UploadResultDto;
import com.netdisk.entity.enums.FileCategoryEnums;
import com.netdisk.entity.enums.FileDelFlagEnums;
import com.netdisk.entity.enums.FileFolderTypeEnums;
import com.netdisk.entity.po.FileInfo;
import com.netdisk.entity.query.FileInfoQuery;
import com.netdisk.entity.vo.FileInfoVO;
import com.netdisk.entity.vo.PaginationResultVO;
import com.netdisk.entity.vo.ResponseVO;
import com.netdisk.exception.BusinessException;
import com.netdisk.utils.CopyTools;
import com.netdisk.utils.StringTools;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件信息 Controller
 */
@RestController("fileInfoController")
@RequestMapping("/file")
@Validated
public class FileInfoController extends CommonFileController {

    /**
     * 根据条件分页查询
     */
    @RequestMapping("/loadDataList")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadDataList(HttpSession session, FileInfoQuery query, String category) {
        FileCategoryEnums categoryEnum = FileCategoryEnums.getByCode(category);
        if (null != categoryEnum) {
            query.setFileCategory(categoryEnum.getCategory());
        }
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        PaginationResultVO result = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
    }

    /**
     * 加载公开文件列表
     */
    @RequestMapping("/loadPublicFileList")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadPublicFileList(FileInfoQuery query, String category) {
        // 设置文件分类（如果有提供分类参数）
        FileCategoryEnums categoryEnum = FileCategoryEnums.getByCode(category);
        if (null != categoryEnum) {
            query.setFileCategory(categoryEnum.getCategory());
        }
        query.setPermission(1);
        query.setAccessType(0);
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        PaginationResultVO<FileInfo> result = fileInfoService.loadPublicFileList(query);
        return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
    }

    /**
     * 获取收藏公开文件列表
     */
    @RequestMapping("/loadCollectPublicFileList")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadCollectPublicFileList(HttpSession session, FileInfoQuery query, String category) {
        FileCategoryEnums categoryEnum = FileCategoryEnums.getByCode(category);
        if (null != categoryEnum) {
            query.setFileCategory(categoryEnum.getCategory());
        }
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setPermission(1);
        query.setAccessType(0);
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        // 分页查询
        PaginationResultVO<FileInfo> result = fileInfoService.loadCollectPublicFileList(query);
        return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
    }

    /**
     * 设置文件公开权限
     */
    @RequestMapping("/setIsPublic")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO setIsPublic(HttpSession session,
                                  @VerifyParam(required = true) String fileId,
                                  Integer permission,
                                  Integer accessType) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.setIsPublic(fileId, webUserDto.getUserId(), permission, accessType);

        // 支持单文件/批量，返回所有最新文件信息
        String[] fileIdArr = fileId.split(",");
        List<FileInfo> fileInfoList = new ArrayList<>();
        for (String fid : fileIdArr) {
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fid.trim(), webUserDto.getUserId());
            if (fileInfo != null) {
                fileInfoList.add(fileInfo);
            }
        }
        return getSuccessResponseVO(fileInfoList);
    }

    /**
     * 修改文件简介
     */
    @RequestMapping ("/saveDescription")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO updateFileDescription(HttpSession session,
                                      @VerifyParam(required = true) String fileId,
                                      String description) {
        System.out.println("fileId=" + fileId + ", description=" + description);
        if (StringUtils.isBlank(fileId)) {
            throw new BusinessException("文件ID不能为空！");
        }
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.updateFileDescription(fileId, webUserDto.getUserId(), description);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/uploadFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO uploadFile(HttpSession session,
                                 String fileId,
                                 MultipartFile file,
                                 @VerifyParam(required = true) String fileName,
                                 @VerifyParam(required = true) String filePid,
                                 @VerifyParam(required = true) String fileMd5,
                                 @VerifyParam(required = true) Integer chunkIndex,
                                 @VerifyParam(required = true) Integer chunks,
                                 Integer permission,
                                 Integer accessType,
                                 String description) {

        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        UploadResultDto resultDto = fileInfoService.uploadFile(webUserDto, fileId, file, fileName, filePid, fileMd5, chunkIndex, chunks,permission,accessType,description);
        return getSuccessResponseVO(resultDto);
    }


    @RequestMapping("/getImage/{imageFolder}/{imageName}")
    public void getImage(HttpServletResponse response, @PathVariable("imageFolder") String imageFolder, @PathVariable("imageName") String imageName) {
        super.getImage(response, imageFolder, imageName);
    }

    @RequestMapping("/ts/getVideoInfo/{fileId}")
    public void getVideoInfo(HttpServletResponse response, HttpSession session, @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webUserDto.getUserId());
    }

    @RequestMapping("/getFile/{fileId}")
    public void getFile(HttpServletResponse response, HttpSession session, @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webUserDto.getUserId());
    }

    @RequestMapping("/getPublicFile/{fileId}")
    public void getPublicFile(HttpServletResponse response, @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        super.getPublicFile(response, fileId);
    }

    /**
     *  新建文件夹
     */
    @RequestMapping("/newFoloder")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO newFoloder(HttpSession session,
                                 @VerifyParam(required = true) String filePid,
                                 @VerifyParam(required = true) String fileName,
                                 Integer permission,
                                 Integer accessType) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.newFolder(filePid, webUserDto.getUserId(), fileName, permission, accessType);
        return getSuccessResponseVO(fileInfo);
    }

    /**
    * 获取文件目录
    */
    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO getFolderInfo(HttpSession session, @VerifyParam(required = true) String path) {
        return super.getFolderInfo(path);
    }


    @RequestMapping("/rename")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO rename(HttpSession session,
                             @VerifyParam(required = true) String fileId,
                             @VerifyParam(required = true) String fileName) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.rename(fileId, webUserDto.getUserId(), fileName);
        return getSuccessResponseVO(CopyTools.copy(fileInfo, FileInfoVO.class));
    }

    @RequestMapping("/loadAllFolder")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadAllFolder(HttpSession session, @VerifyParam(required = true) String filePid, String currentFileIds) {
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setFilePid(filePid);
        query.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        if (!StringTools.isEmpty(currentFileIds)) {
            query.setExcludeFileIdArray(currentFileIds.split(","));
        }
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        query.setOrderBy("create_time desc");
        List<FileInfo> fileInfoList = fileInfoService.findListByParam(query);
        return getSuccessResponseVO(CopyTools.copyList(fileInfoList, FileInfoVO.class));
    }

    @RequestMapping("/changeFileFolder")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO changeFileFolder(HttpSession session,
                                       @VerifyParam(required = true) String fileIds,
                                       @VerifyParam(required = true) String filePid) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.changeFileFolder(fileIds, filePid, webUserDto.getUserId());
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/createDownloadUrl/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO createDownloadUrl(HttpSession session, @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        return super.createDownloadUrl(fileId, getUserInfoFromSession(session).getUserId());
    }

    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public void download(HttpServletRequest request, HttpServletResponse response,
                         @PathVariable("code") @VerifyParam(required = true) String code) throws Exception {
        super.download(request, response, code);
    }

    @RequestMapping("/createPublicDownloadUrl/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO createPublicDownloadUrl(@PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        return super.createPublicDownloadUrl(fileId);
    }

    @RequestMapping("/publicDownload/{code}")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public void publicDownload(HttpServletRequest request, HttpServletResponse response,
                               @PathVariable("code") @VerifyParam(required = true) String code) throws Exception {
        super.publicDownload(request, response, code);
    }

    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO delFile(HttpSession session, @VerifyParam(required = true) String fileIds) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.removeFile2RecycleBatch(webUserDto.getUserId(), fileIds);
        return getSuccessResponseVO(null);
    }

}