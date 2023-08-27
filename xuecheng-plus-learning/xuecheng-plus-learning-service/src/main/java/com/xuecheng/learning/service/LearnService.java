package com.xuecheng.learning.service;

import com.xuecheng.base.model.RestResponse;

/**
 * @description 学习过程管理Service接口
 */
public interface LearnService {

    /**
     * @description 获取章节对应的教学视频
     * @param userId 用户id
     * @param courseId 课程id
     * @param teachplanId 章节id
     * @param mediaId 视频文件id
     * @return RestResponse
     */
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId);

}
