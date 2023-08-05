package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author 12508
 */
@Slf4j
@Component
public class VideoTask {

    @Autowired
    MediaFileService mediaFileService;

    @Autowired
    MediaFileProcessService mediaFileProcessService;

    @Value("${videoprocess.ffmpegpath}")
    String ffmpegPath;

    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex(); //当前分片序号
        int shardTotal = XxlJobHelper.getShardTotal(); //分片总数
        List<MediaProcess> mediaProcessList = null;
        int size = 0;
        try {
            // 取出cpu核心数作为一次处理数据的条数
            int processors = Runtime.getRuntime().availableProcessors();
            // 一次处理视频数量不要超过cpu核心数
            mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors);
            size = mediaProcessList.size();
            log.debug("取出待处理视频任务{}条", size);
            if (0 == size) {
                return; // 如果没有要处理的视频任务, 退出
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 启动size个线程的线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(size);
        // 计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        // 将处理任务加入线程池
        mediaProcessList.forEach(mediaProcess -> {
            threadPool.execute(() ->{ // 瞬间启动size个线程
                try {
                    // 任务id
                    Long taskId = mediaProcess.getId();
                    // 抢占任务
                    boolean b = mediaFileProcessService.startTask(taskId);
                    if (!b) {
                        return; //如果b为false, 那么说明当前线程抢占该任务失败, 所以在这直接返回
                    }
                    log.debug("开始执行任务:{}", mediaProcess);
                    // 下面是处理逻辑
                    // 桶
                    String bucket = mediaProcess.getBucket();
                    // 存储路径
                    String filePath = mediaProcess.getFilePath();
                    // 原始视频的md5值
                    String fileId = mediaProcess.getFileId();
                    // 原始文件名称
                    String filename = mediaProcess.getFilename();
                    // 将要处理的文件临时下载到服务器上
                    File originalFile = mediaFileService.downloadFileFromMinIO(bucket, filePath);
                    if (originalFile == null) {
                        log.debug("下载待处理文件失败, originalFile:{}", bucket.concat(filePath));
                        // 任务处理失败, 更新任务处理状态
                        mediaFileProcessService.saveProcessFinishStatus(
                                mediaProcess.getId(), "3", fileId, null, "下载待处理文件失败");
                        return;
                    }
                    // 处理结束的视频文件
                    File mp4File = null;
                    try {
                        mp4File = File.createTempFile("mp4", ".mp4");
                    } catch (IOException e) {
                        log.error("创建MP4临时文件失败");
                        // 任务处理失败, 更新任务处理状态
                        mediaFileProcessService.saveProcessFinishStatus(
                                mediaProcess.getId(), "3", fileId, null, "创建MP4临时文件失败");
                        return; // 返回
                    }

                    // 视频处理结果
                    String result = "";
                    try {
                        // 开始处理视频
                        Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegPath, originalFile.getAbsolutePath(),
                                mp4File.getName(), mp4File.getAbsolutePath());
                        // 开始视频转换, 成功将返回success
                        result = videoUtil.generateMp4();
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("处理视频文件:{}, 出错:{}", mediaProcess.getFilePath(), e.getMessage());
                    }
                    if (!result.equals("success")) {
                        // 记录错误信息
                        log.error("处理视频失败, 视频地址是:{}, 错误信息:{}", bucket + filePath, result);
                        // 任务处理失败, 更新任务处理状态
                        mediaFileProcessService.saveProcessFinishStatus(
                                mediaProcess.getId(), "3", fileId, null, result);
                        return; //返回
                    }

                    // 将转换好的mp4文件上传到minio
                    // mp4在minio的存储路径
                    String objectName = getFilePath(fileId, ".mp4");
                    // 访问url
                    String url = "/" + bucket + "/" + objectName;
                    try {
                        // 上传到minio
                        mediaFileService.addMediaFilesToMinio(mp4File.getAbsolutePath(),
                                "video/mp4", bucket, objectName);
                        // 任务处理成功, 更新任务处理状态为成功, 删除任务处理状态表中的记录, 并添加此记录到任务处理历史表
                        mediaFileProcessService.saveProcessFinishStatus(
                                mediaProcess.getId(), "2", fileId, url, null);

                    } catch (Exception e) {
                        log.error("上传视频失败或入库失败, 视频地址:{},错误信息:{}", bucket + objectName, e.getMessage());
                        // 最终还是上传失败, 更新任务处理状态为失败
                        mediaFileProcessService.saveProcessFinishStatus(
                                mediaProcess.getId(), "3", fileId, url, "处理后视频上传后入库失败");
                    }
                } finally { // 总之就是计数器减1
                    countDownLatch.countDown(); //计数器减1
                }
            });
        });
        // 等待, 给一个充裕的超时时间, 防止无限等待, 到达超时时间还没有处理完成则结束任务, 等待size个线程执行完后也就是计数器为0时放行
        countDownLatch.await(30, TimeUnit.MINUTES); // 最久等待30分钟, 超过30分钟就结束这个方法
    }

    private String getFilePath(String fileId, String fileExt) {
        return fileId.charAt(0) + "/" + fileId.charAt(1) + "/" + fileId + "/" + fileId + fileExt;
    }

}
