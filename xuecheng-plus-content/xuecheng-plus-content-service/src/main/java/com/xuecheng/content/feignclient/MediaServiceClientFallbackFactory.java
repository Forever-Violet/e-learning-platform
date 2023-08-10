package com.xuecheng.content.feignclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 *  第一种方法即定义MediaServiceClientFallback实现MediaServiceClient接口无法取出熔断所抛出的异常
 *  第二种方法定义MediaServiceClientFallbackFactory 可以解决这个问题。
 */
@Slf4j
@Component
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient> {

    @Override
    public MediaServiceClient create(Throwable throwable) {
        return new MediaServiceClient() {
            @Override
            public String upload(MultipartFile filedata, String objectName) throws IOException {
                // 熔断后的降级方法
                log.debug("调用媒资管理服务上传文件时发生熔断，异常信息:{}", throwable.toString(), throwable);
                return null; //返回一个null对象, 上游服务请求接口得到一个null说明执行了降级处理。
            }
        };
    }
}
