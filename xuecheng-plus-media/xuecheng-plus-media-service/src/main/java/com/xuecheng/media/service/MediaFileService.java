package com.xuecheng.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;

import java.io.File;

/**
 * @author 12508
 * @description 媒资文件管理业务类
 * @date 2022/9/10 8:55
 * @version 1.0
 */
public interface MediaFileService extends IService<MediaFiles> {

    /**
     * @description 媒资文件查询方法
     * @param pageParams 分页参数
     * @param queryMediaParamsDto 查询条件
     * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
     * @author Mr.M
     * @date 2022/9/10 8:57
     */
    public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

    /**
     * @description 上传文件
     * @param companyId 机构id
     * @param uploadFileParamsDto 上传文件信息
     * @param localFilePath 文件磁盘路径
     * @return 文件信息
     */
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath);

    /**
     * @description 将文件信息添加到文件表
     * @param companyId 机构id
     * @param fileMd5 文件md5值
     * @param uploadFileParamsDto 上传文件的信息
     * @param bucket 桶
     * @param objectName 对象名称
     * @return MediaFiles
     */
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto
            , String bucket, String objectName);

    /**
     * @description 检查文件是否已经存在
     * @param fileMd5 文件的md5值
     * @return RestResponse<Boolean>
     */
    RestResponse<Boolean> checkFile(String fileMd5);

    /**
     * @description 检查分块文件是否存在
     * @param fileMd5 整体文件的md5值
     * @param chunkIndex 分块文件序号
     * @return RestResponse<Boolean>
     */
    RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex);

    /**
     * @description 上传分块文件
     * @param fileMd5 整体文件md5值
     * @param chunk 分块文件的序号
     * @param localChunkFilePath 分块文件暂存到本地的路径
     * @return RestResponse
     */
    RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath);


    /**
     * @description 合并分块
     * @param companyId 机构id
     * @param fileMd5 文件md5值
     * @param chunkTotal 分块文件的总数量
     * @param uploadFileParamsDto 文件信息
     * @return RestResponse
     */
    RestResponse mergeChunk(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto);


    /**
     * @description 从minio下载文件
     * @param bucket 桶
     * @param objectName 对象名称
     * @return 下载后的文件
     */
    File downloadFileFromMinIO(String bucket, String objectName);

    /**
     * @description 将文件写入minio
     * @param localFilePath 文件地址
     * @param mimeType mimeType
     * @param bucket 桶
     * @param objectName 对象名称
     * @return boolean
     */
    boolean addMediaFilesToMinio(String localFilePath, String mimeType, String bucket, String objectName);


}
