package xuecheng.media;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @description 大文件处理测试
 */
public class BigFileTest {

    // 测试文件分块方法
    @Test
    public void testChunk() throws IOException {
        File sourceFile = new File("C:\\Users\\12508\\Videos\\2023-03-24 23-04-32.mp4");
        String chunkPath = "G:\\chunk\\";
        File chunkFolder = new File(chunkPath);
        if (!chunkFolder.exists()) {
            chunkFolder.mkdirs();
        }
        // 分块大小
        long chunkSize = 1024 * 1024 * 5; // 1024字节 * 1024 * 5= 5MB
        // 分块数量
        long chunkNum = (long) Math.ceil(sourceFile.length() * 1.0 / chunkSize); //向上取整, 即如果是3.2, 那么取4
        System.out.println("分块总数: " + chunkNum);
        // 缓冲区大小
        byte[] b = new byte[1024];
        // 使用RandomAccessFile访问文件, 从源文件中读取数据, 向分块文件中写数据
        RandomAccessFile raf_read = new RandomAccessFile(sourceFile, "r"); //读
        // 分块
        for (int i = 0; i < chunkNum; i++) {
            // 创建分块文件
            File file = new File(chunkPath + i);
            if (file.exists()) { //如果当前分块文件已经存在
                file.delete(); //那么删掉
            }
            boolean newFile = file.createNewFile(); //是否成功创建新的分块文件
            if (newFile) {
                // 向分块文件中写数据
                RandomAccessFile raf_write = new RandomAccessFile(file, "rw"); //读写
                int len = -1;
                while((len = raf_read.read(b)) != -1){ //从源文件中读到b中
                    raf_write.write(b, 0, len); // 从b中写到file中
                    if (file.length() >= chunkSize) { // 如果分块文件大小等于设定的分块大小
                        break;
                    }
                }
                raf_write.close(); //关闭写流
                System.out.println("完成分块" + i);
            }
        }
        raf_read.close(); //关闭读流
    }

    //测试文件合并方法
    @Test
    public void testMerge() throws IOException {
        // 块文件目录
        File chunkFolder = new File("G:\\chunk\\");
        // 原始文件
        File originalFile = new File("C:\\Users\\12508\\Videos\\2023-03-24 23-04-32.mp4");
        // 合并文件
        File mergeFile = new File("G:\\1.mp4");
        if (mergeFile.exists()) {
            mergeFile.delete();
        }
        // 创建新的合并文件
        mergeFile.createNewFile();
        // 用于写文件
        RandomAccessFile raf_write = new RandomAccessFile(mergeFile, "rw");
        // 指针指向文件顶端
        raf_write.seek(0);
        // 缓冲区
        byte[] bytes = new byte[1024];
        // 分块列表
        File[] fileArray = chunkFolder.listFiles();
        // 转成集合, 便于排序
        List<File> fileList = Arrays.asList(fileArray);
        // 从小到大排序
        Collections.sort(fileList, new Comparator<File>() {
            /**
             * 如果o1在排序结果中应该排在o2之前，那么比较器的compare方法应该返回一个负数。
             * 如果o1在排序结果中应该排在o2之后，那么比较器的compare方法应该返回一个正数。
             */
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
            }
        });
        // 合并文件
        for (File chunkFile : fileList) {
            RandomAccessFile raf_read = new RandomAccessFile(chunkFile, "rw");
            int len = -1;
            while ((len = raf_read.read(bytes)) != -1) {
                raf_write.write(bytes, 0, len);
            }
            raf_read.close();
        }
        raf_write.close();

        // 校验文件
        try (
                FileInputStream fileInputStream = new FileInputStream(originalFile);
                FileInputStream mergeFileStream = new FileInputStream(mergeFile);
                ) {
            // 取出原始文件的md5
            String originalMd5 = DigestUtils.md5Hex(fileInputStream);
            // 取出合并问价拿到md5值进行比较
            String mergeMd5 = DigestUtils.md5Hex(mergeFileStream);
            if (originalMd5.equals(mergeMd5)) {
                System.out.println("合并文件成功");
            } else {
                System.out.println("合并文件失败");
            }
        }
    }



}
