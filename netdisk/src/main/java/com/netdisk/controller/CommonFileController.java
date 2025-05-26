package com.netdisk.controller;

import com.netdisk.component.RedisComponent;
import com.netdisk.entity.config.AppConfig;
import com.netdisk.entity.constants.Constants;
import com.netdisk.entity.dto.DownloadFileDto;
import com.netdisk.entity.enums.FileCategoryEnums;
import com.netdisk.entity.enums.FileFolderTypeEnums;
import com.netdisk.entity.enums.ResponseCodeEnum;
import com.netdisk.entity.po.FileInfo;
import com.netdisk.entity.query.FileInfoQuery;
import com.netdisk.entity.vo.FolderVO;
import com.netdisk.entity.vo.ResponseVO;
import com.netdisk.exception.BusinessException;
import com.netdisk.service.FileInfoService;
import com.netdisk.utils.CopyTools;
import com.netdisk.utils.StringTools;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URLEncoder;
import java.util.List;

public class CommonFileController extends ABaseController {

    @Resource
    protected FileInfoService fileInfoService;

    @Resource
    protected AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;


    public ResponseVO getFolderInfo(String path) {
        String[] pathArray = path.split("/");
        FileInfoQuery infoQuery = new FileInfoQuery();
        //infoQuery.setUserId(userId);
        infoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        infoQuery.setFileIdArray(pathArray);
        String orderBy = "field(file_id,\"" + StringUtils.join(pathArray, "\",\"") + "\")";
        infoQuery.setOrderBy(orderBy);
        List<FileInfo> fileInfoList = fileInfoService.findListByParam(infoQuery);
        return getSuccessResponseVO(CopyTools.copyList(fileInfoList, FolderVO.class));
    }

    public void getImage(HttpServletResponse response, String imageFolder, String imageName) {
        if (StringTools.isEmpty(imageFolder) || StringUtils.isBlank(imageName)) {
            return;
        }
        String imageSuffix = StringTools.getFileSuffix(imageName);
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + imageFolder + "/" + imageName;
        imageSuffix = imageSuffix.replace(".", "");
        String contentType = "image/" + imageSuffix;
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "max-age=2592000");
        readFile(response, filePath);
    }

    protected void getFile(HttpServletResponse response, String fileId, String userId) {
        String filePath = null;
        if (fileId.endsWith(".ts")) {
            String[] tsAarray = fileId.split("_");
            String realFileId = tsAarray[0];
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(realFileId, userId);
            if (fileInfo == null) {
                return;
            }
            String fileName = fileInfo.getFilePath();
            fileName = StringTools.getFileNameNoSuffix(fileName) + "/" + fileId;
            filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileName;
        } else {
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId, userId);
            if (fileInfo == null) {
                return;
            }
            //视频文件读取.m3u8文件
            if (FileCategoryEnums.VIDEO.getCategory().equals(fileInfo.getFileCategory())) {
                //重新设置文件路径
                String fileNameNoSuffix = StringTools.getFileNameNoSuffix(fileInfo.getFilePath());
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileNameNoSuffix + "/" + Constants.M3U8_NAME;
            } else {
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileInfo.getFilePath();
            }
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        readFile(response, filePath);
    }
    /**
     * 预览公开文件（直链，无需登录，仅校验公开权限）
     */
    protected void getPublicFile(HttpServletResponse response, String fileId) {
        // 1. 查询文件信息（只查fileId）
        FileInfo fileInfo = fileInfoService.getFileInfoByFileId(fileId);
        // 2. 权限校验：公开+只读+不是文件夹
        if (fileInfo == null
                || !Integer.valueOf(1).equals(fileInfo.getPermission())
                || !Integer.valueOf(0).equals(fileInfo.getAccessType())
                || FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
            response.setStatus(403);
            return;
        }
        // 3. 计算本地文件路径（兼容ts、m3u8、普通文件）
        String filePath;
        if (fileId.endsWith(".ts")) {
            String[] tsArray = fileId.split("_");
            String realFileId = tsArray[0];
            FileInfo tsFileInfo = fileInfoService.getFileInfoByFileId(realFileId);
            if (tsFileInfo == null) {
                response.setStatus(404);
                return;
            }
            String fileName = tsFileInfo.getFilePath();
            fileName = StringTools.getFileNameNoSuffix(fileName) + "/" + fileId;
            filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileName;
        } else {
            if (FileCategoryEnums.VIDEO.getCategory().equals(fileInfo.getFileCategory())) {
                String fileNameNoSuffix = StringTools.getFileNameNoSuffix(fileInfo.getFilePath());
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileNameNoSuffix + "/" + Constants.M3U8_NAME;
            } else {
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileInfo.getFilePath();
            }
        }
        File file = new File(filePath);
        if (!file.exists()) {
            response.setStatus(404);
            return;
        }
        // 4. 设置 Content-Type 和 Content-Disposition
        String fileName = fileInfo.getFileName();
        String fileSuffix = StringTools.getFileSuffix(fileName);
        if (fileSuffix != null && (fileSuffix.equalsIgnoreCase(".png") || fileSuffix.equalsIgnoreCase(".jpg") ||
                fileSuffix.equalsIgnoreCase(".jpeg") || fileSuffix.equalsIgnoreCase(".gif"))) {
            response.setContentType("image/" + fileSuffix.replace(".", ""));
        } else if (fileSuffix != null && fileSuffix.equalsIgnoreCase(".pdf")) {
            response.setContentType("application/pdf");
        } else {
            response.setContentType("application/octet-stream");
        }
        response.setHeader("Content-Disposition", "inline;filename=\"" + fileName + "\"");
        // 5. 输出文件流
        readFile(response, filePath);
    }

    protected ResponseVO createDownloadUrl(String fileId, String userId) {
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String code = StringTools.getRandomString(Constants.LENGTH_50);
        DownloadFileDto downloadFileDto = new DownloadFileDto();
        downloadFileDto.setDownloadCode(code);
        downloadFileDto.setFilePath(fileInfo.getFilePath());
        downloadFileDto.setFileName(fileInfo.getFileName());

        redisComponent.saveDownloadCode(code, downloadFileDto);

        return getSuccessResponseVO(code);
    }

    protected void download(HttpServletRequest request, HttpServletResponse response, String code) throws Exception {
        DownloadFileDto downloadFileDto = redisComponent.getDownloadCode(code);
        if (null == downloadFileDto) {
            return;
        }
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + downloadFileDto.getFilePath();
        String fileName = downloadFileDto.getFileName();
        response.setContentType("application/octet-stream; charset=UTF-8");
        if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0) {//IE浏览器
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } else {
            fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
        }
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        readFile(response, filePath);
    }
    protected ResponseVO createPublicDownloadUrl(String fileId) {
        // 查询文件（不需要 userId，只需要 fileId，且必须是公开文件并非文件夹）
        FileInfo fileInfo = fileInfoService.getFileInfoByFileId(fileId);
        if (fileInfo == null
                || !Integer.valueOf(1).equals(fileInfo.getPermission())   // 公开
                || !Integer.valueOf(0).equals(fileInfo.getAccessType())   // 只读下载
                || FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String code = StringTools.getRandomString(Constants.LENGTH_50);
        DownloadFileDto downloadFileDto = new DownloadFileDto();
        downloadFileDto.setDownloadCode(code);
        downloadFileDto.setFileId(fileInfo.getFileId());
        downloadFileDto.setFilePath(fileInfo.getFilePath());
        downloadFileDto.setFileName(fileInfo.getFileName());

        redisComponent.saveDownloadCode(code, downloadFileDto);

        return getSuccessResponseVO(code);
    }
    protected void publicDownload(HttpServletRequest request, HttpServletResponse response, String code) throws Exception {
        DownloadFileDto downloadFileDto = redisComponent.getDownloadCode(code);
        if (downloadFileDto == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write("下载链接已失效或不存在");
            return;
        }
        // 重新校验文件是否依然公开
        FileInfo fileInfo = fileInfoService.getFileInfoByFileId(downloadFileDto.getFileId());
        if (fileInfo == null
                || !Integer.valueOf(1).equals(fileInfo.getPermission())
                || !Integer.valueOf(0).equals(fileInfo.getAccessType())
                || FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write("文件未公开或已失效，无法下载");
            return;
        }
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + downloadFileDto.getFilePath();
        String fileName = downloadFileDto.getFileName();
        response.setContentType("application/octet-stream; charset=UTF-8");
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.toLowerCase().contains("msie")) {
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } else {
            fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
        }
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write("文件不存在");
            return;
        }
        readFile(response, filePath);
    }
}
