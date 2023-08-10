package com.xuecheng.content;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @description 测试使用feign远程上传文件
 */
@SpringBootTest
public class FeignUploadTest {

    @Autowired
    MediaServiceClient mediaServiceClient;

    // 远程调用, 上传文件
    @Test
    public void test() throws IOException {
        // 将File类型转成MultipartFile类型再上传
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(new File("G:\\test.html"));
        mediaServiceClient.upload(multipartFile, "course/120.html");
    }

}
