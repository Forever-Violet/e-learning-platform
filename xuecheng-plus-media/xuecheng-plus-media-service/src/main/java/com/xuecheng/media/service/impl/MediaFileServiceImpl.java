package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @author 12508
 * @description TODO
 * @version 1.0
 */
@Service
@Slf4j
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MinioClient minioClient;

    @Autowired
    MediaFileService currentProxy; // 代理对象, 在非事务方法中调用同类下事务方法, 使事务生效

    //普通文件桶 bucket
    @Value("${minio.bucket.files}")
    private String bucket_Files;

    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .like(StringUtils.isNotEmpty(queryMediaParamsDto.getFilename())
                        , MediaFiles::getFilename, queryMediaParamsDto.getFilename()) // 模糊查询名称
                .eq(StringUtils.isNotEmpty(queryMediaParamsDto.getFileType())
                        , MediaFiles::getFileType, queryMediaParamsDto.getFileType()); // 类型匹配

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    // 获取(首次则创建)文件默认存储目录路径 年/月/日
    private String getDefaultFolderPath(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String folder = sdf.format(new Date()).replace("-", "/") + "/";
        return folder;
    }

    // 获取文件的md5值
    private String getFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            String fileMd5 = DigestUtils.md5Hex(fileInputStream);
            return fileMd5;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 获取文件的mimeType
    private String getMimeType(String extension) {
        if (extension == null) {
            extension = "";
        }
        // 根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        // 通用mimeType, 字节流
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }

    /**
     * @description 将文件写入minio
     * @param localFilePath 文件地址
     * @param mimeType mimeType
     * @param bucket 桶
     * @param objectName 对象名称
     * @return boolean
     */
    public boolean addMediaFilesToMinio(String localFilePath, String mimeType, String bucket, String objectName) {
        try {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket(bucket)
                    .filename(localFilePath)
                    .object(objectName)
                    .contentType(mimeType)
                    .build();
            minioClient.uploadObject(uploadObjectArgs); //上传
            log.debug("上传文件到minio成功,bucket:{},objectName:{}", bucket, objectName);
            System.out.println("上传成功");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件到minio出错,bucket:{},objectName:{},错误原因:{}", bucket, objectName, e.getMessage(), e);
            XueChengPlusException.cast("上传文件到文件系统失败");
        }
        return false;
    }

    /**
     * @description 将文件信息添加到文件表
     * @param companyId 机构id
     * @param fileMd5 文件md5值
     * @param uploadFileParamsDto 上传文件的信息
     * @param bucket 桶
     * @param objectName 对象名称
     * @return MediaFiles
     */
    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto
            , String bucket, String objectName) {
        // 从数据库查询文件, 文件的md5值就是其信息的主键id
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            // 拷贝基本信息
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            // id
            mediaFiles.setId(fileMd5);
            // 文化部ud
            mediaFiles.setFileId(fileMd5);
            // 桶
            mediaFiles.setBucket(bucket);
            // 机构id
            mediaFiles.setCompanyId(companyId);
            // url
            mediaFiles.setUrl("/" + bucket + "/" + objectName);
            // 存储路径
            mediaFiles.setFilePath(objectName);
            // 创建时间
            mediaFiles.setCreateDate(LocalDateTime.now());
            // 审核状态
            mediaFiles.setAuditStatus("002003");
            // 状态
            mediaFiles.setStatus("1");
            // 保存文件信息到文件表
            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert <= 0) {
                log.error("保存文件信息到数据库失败,{}", mediaFiles.toString());
                XueChengPlusException.cast("保存文件信息失败");
                return null;
            }
            log.debug("保存文件信息到数据库成功,{}", mediaFiles.toString());
        }
        return mediaFiles;
    }

    /**
     * @description
     * @param companyId 机构id
     * @param uploadFileParamsDto 上传文件信息
     * @param localFilePath 文件磁盘路径
     * @return
     */
    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {
        File file = new File(localFilePath);
        if (!file.exists()) {
            XueChengPlusException.cast("文件不存在");
        }
        // 文件名称
        String filename = uploadFileParamsDto.getFilename();
        // 文件扩展名
        String extension = filename.substring(filename.lastIndexOf(".")); //最后一个.的位置, 往后截取
        // 文件mimeType
        String mimeType = getMimeType(extension);
        // 文件的md5值
        String fileMd5 = getFileMd5(file);
        // 文件的默认目录
        String defaultFolderPath = getDefaultFolderPath();
        // 存储到minio中的对象名(带目录)
        String objectName = defaultFolderPath + fileMd5 + extension;
        // 将文件上传到minio
        boolean b = addMediaFilesToMinio(localFilePath, mimeType, bucket_Files, objectName);
        // 设置文件大小
        uploadFileParamsDto.setFileSize(file.length());
        // 将文件信息存储到数据库, 使用代理对象去执行同类下的事务方法, 使事务生效, 因为在同类下调用的事务方法使用的不是代理对象而是原始类
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_Files, objectName);
        if (mediaFiles == null) {
            XueChengPlusException.cast("文件上传后保存信息失败");
        }
        // 准备返回数据
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
        return uploadFileResultDto;

    }
}
