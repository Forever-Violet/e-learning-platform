package xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * @description 测试Minio
 */
public class MinioTest {

    static MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://localhost:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    // 上传文件
    @Test
    public void upload() {
        // 根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".mp4");
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE; //通用mimeType, 字节流
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }

        try{
            UploadObjectArgs testbucketObject = UploadObjectArgs.builder()
                    .bucket("testbucket")
                    .filename("C:\\Users\\12508\\Videos\\2023-03-24 23-04-32.mp4") //指定本地文件路径
                    .object("test.mp4") //对象名
                    .contentType(mimeType) //默认根据扩展名确认文件内容类型, 也可以指定
                    .build();
            minioClient.uploadObject(testbucketObject);
            System.out.println("上传成功");
        }catch (Exception e) {
            e.printStackTrace();
            System.out.println("上传失败");
        }
    }

    // 删除文件
    @Test
    public void delete() {
        try{
            RemoveObjectArgs removeObject = RemoveObjectArgs.builder()
                    .bucket("testbucket")
                    .object("test.mp4") //对象名
                    .build();
            minioClient.removeObject(removeObject);
            System.out.println("删除成功");
        }catch (Exception e) {
            e.printStackTrace();
            System.out.println("删除失败");
        }
    }

    // 查询文件
    @Test
    public void getFile() throws Exception{
        GetObjectArgs getObjectArgs = GetObjectArgs
                .builder()
                .bucket("testbucket")
                .object("1690111608463.jpg") //对象名
                .build();

        // 查询远程服务器获取到的一个流对象
        GetObjectResponse inputStream = minioClient.getObject(getObjectArgs);
        // 指定输出流
        FileOutputStream outputStream = new FileOutputStream(new File("G:\\MinioAndData\\1.jpg"));
        IOUtils.copy(inputStream, outputStream);

        // 校验文件的完整性, 对文件的内容进行md5校验
        // 上传到文件系统的原文件
        String source_md5 = DigestUtils.md5Hex(new FileInputStream(new File("C:\\Users\\12508\\Pictures\\1690111608463.jpg")));
        // 从文件系统中下载的文件
        String local_md5 = DigestUtils.md5Hex(new FileInputStream(new File("G:\\MinioAndData\\1.jpg")));
        if (source_md5.equals(local_md5)) {
            System.out.println("下载成功");
        }

    }

}
