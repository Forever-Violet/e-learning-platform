package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author 12508
 * @description TODO
 * @version 1.0
 */
@Service
@Slf4j
public class MediaFileServiceImpl extends ServiceImpl<MediaFilesMapper, MediaFiles>  implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MinioClient minioClient;

    @Autowired
    MediaProcessMapper mediaProcessMapper;

    @Autowired
    MediaFileService currentProxy; // 代理对象, 在非事务方法中调用同类下事务方法, 使事务生效

    //普通文件桶 bucket
    @Value("${minio.bucket.files}")
    private String bucket_Files;

    @Value("${minio.bucket.videofiles}")
    private String bucket_videoFiles;

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

            // 添加到待处理任务表
            addWaitingTask(mediaFiles);
            log.debug("保存文件信息到数据库成功,{}", mediaFiles.toString());
        }
        return mediaFiles;
    }

    /**
     * 添加待处理任务
     * @param mediaFiles 媒资文件信息 (会判断是否为.avi视频, 如果是即需要转码, 添加到待处理任务表)
     */
    private void addWaitingTask(MediaFiles mediaFiles) {
        // 文件名称
        String filename = mediaFiles.getFilename();
        // 文件扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        // 获取文件mimeType
        String mimeType = getMimeType(extension);
        // 如果mimeType是avi视频的, 那么添加到视频待处理表
        if ("video/x-msvideo".equals(mimeType)) {
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles, mediaProcess);
            mediaProcess.setStatus("1"); //设置状态为未处理
            mediaProcess.setFailCount(0); //设置失败次数默认为0
            mediaProcessMapper.insert(mediaProcess); //插入待处理任务表
        }

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


    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        // 查询文件信息
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles != null) {
            // 桶
            String bucket = mediaFiles.getBucket();
            // 存储目录
            String filePath = mediaFiles.getFilePath();
            // 文件流
            InputStream stream = null;
            try {
                stream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucket)
                                .object(filePath)
                                .build()
                );
                if (stream != null) {
                    // 文件已经存在
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 文件不存在
        return RestResponse.success(false);
    }

    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
        //得到分块文件的目录
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //得到分块文件的路径
        String chunkFilePath = chunkFileFolderPath + chunkIndex;

        // 文件流
        InputStream fileInputStream = null;
        try {
            fileInputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket_videoFiles)
                            .object(chunkFilePath)
                            .build()
            );
            if (fileInputStream != null) {
                // 分块文件已经存在
                return RestResponse.success(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //分块文件不存在
        return RestResponse.success(false);
    }

    private String getChunkFileFolderPath(String fileMd5) {
        // 例如7ad413b389039fc134a674ca23bb63a9, 得到的分块文件目录为 7/a/7ad413b389039fc134a674ca23bb63a9/chunk/
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + "chunk" + "/";
    }


    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {
        // 得到分块文件的目录路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        // 得到分块文件的路径
        String chunkFilePath = chunkFileFolderPath + chunk;
        // 定义一个mimeType, 返回一个通用的mimeType
        String mimeType = getMimeType(null);

        // 将分块文件上传到minIO
        boolean b = addMediaFilesToMinio(localChunkFilePath, mimeType, bucket_videoFiles, chunkFilePath);
        if (!b) {
            return RestResponse.validfail(false, "上传分块文件失败");
        }
        // 上传成功
        return RestResponse.success(true);
    }

    @Override
    public RestResponse mergeChunk(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        // 获取分块文件在minio中的目录
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        // 将全部分块文件在minio中的路径组成列表 List<ComposeSource>
        List<ComposeSource> sourceList = Stream.iterate(0, i -> ++i)
                .limit(chunkTotal) //限制流的大小
                .map(i -> ComposeSource.builder()
                        .bucket(bucket_videoFiles)
                        .object(chunkFileFolderPath.concat(Integer.toString(i)))
                        .build())
                .collect(Collectors.toList()); //收集成list
        // 合并
        // 文件名称
        String fileName = uploadFileParamsDto.getFilename();
        // 文件扩展名
        String extName = fileName.substring(fileName.lastIndexOf("."));
        // 合并后的文件路径
        String mergeFilePath = getFilePathByMd5(fileMd5, extName);
        try {
            // 合并文件
            ObjectWriteResponse response = minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(bucket_videoFiles)
                            .object(mergeFilePath)
                            .sources(sourceList)
                            .build()
            );
            log.debug("合并文件成功:{}", mergeFilePath);
        } catch (Exception e) {
            log.debug("合并文件失败,fileMd5:{},异常:{}", fileMd5, e.getMessage(), e);
            return RestResponse.validfail(false, "文件合并失败");
        }
        // 验证Md5值 临时用来校验的文件
        File minioFile = downloadFileFromMinIO(bucket_videoFiles, mergeFilePath);
        if (minioFile == null) {
            log.debug("下载合并后文件失败,mergeFilePath:{}", mergeFilePath);
            return RestResponse.validfail(false, "下载合并后文件失败");
        }

        try (InputStream newFileInputStream = new FileInputStream(minioFile)) { //把input流放在try()里边, 用完会自动关闭
            // 从minio下载的文件的md5值
            String md5Hex = DigestUtils.md5Hex(newFileInputStream);
            // 比较md5值, 不一致说明文件不完整
            if (!fileMd5.equals(md5Hex)) {
                return RestResponse.validfail(false, "文件合并校验失败, 最终上传失败。");
            }
            // 设置文件大小
            uploadFileParamsDto.setFileSize(minioFile.length());
        } catch (Exception e) {
            log.debug("校验文件失败,fileMd5:{},异常:{}", fileMd5, e.getMessage(), e);
            return RestResponse.validfail(false, "文件合并校验失败, 最终上传失败。");
        } finally {
            if (minioFile != null) { //删掉临时用来校验的文件
                minioFile.delete();
            }
        }

        // 文件入库
        currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_videoFiles, mergeFilePath);

        // 清除分块文件
        clearChunkFiles(chunkFileFolderPath, chunkTotal);
        return RestResponse.success(true);

    }

    /**
     * @description 清除分块文件
     * @param chunkFileFolderPath 分块文件目录
     * @param chunkTotal 分块文件总数
     */
    private void clearChunkFiles(String chunkFileFolderPath, int chunkTotal) {

        try {
            List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                    .limit(chunkTotal)                          //concat拼接字符串
                    .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
                    .collect(Collectors.toList()); //收集成list
            // 注意是RemoveObjectsArgs
            RemoveObjectsArgs removeObjectArgs = RemoveObjectsArgs.builder()
                    .bucket(bucket_videoFiles)
                    .objects(deleteObjects)
                    .build();
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectArgs);
            results.forEach(r -> {
                DeleteError deleteError = null;
                try {
                    deleteError = r.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("清除分块文件失败,objectName:{}",deleteError.objectName(), e);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            log.error("清除分块文件失败,chunkFileFolderPath:{}", chunkFileFolderPath, e);
        }

    }

    /**
     * @description 从minio下载文件
     * @param bucket 桶
     * @param objectName 对象名称
     * @return 下载后的文件
     */
    public File downloadFileFromMinIO(String bucket, String objectName) {
        // 临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
            // 创建临时文件
            minioFile = File.createTempFile("minio", ".merge");//临时文件的名称中间会随机生成数字保证不重复
            outputStream = new FileOutputStream(minioFile); //输出流绑定到minioFile
            IOUtils.copy(stream, outputStream); //复制到minioFile
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) { //关闭流
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 得到合并后的文件的路径
     * @param fileMd5 文件的id即md5值
     * @param extName 文件扩展名
     */
    private String getFilePathByMd5(String fileMd5, String extName) {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + fileMd5 + extName;
    }
}
