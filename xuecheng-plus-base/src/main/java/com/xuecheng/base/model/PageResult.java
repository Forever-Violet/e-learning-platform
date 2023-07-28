package com.xuecheng.base.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * @description 分页查询结果模型类
 * @author 12508
 * 实现Serializable接口的主要目的是为了将一个对象转换为字节序列，
 * 从而可以在网络上传输或保存到本地文件系统。当一个类实现Serializable接口时，
 * 它就变得可序列化，这允许该类的对象在内存中以字节形式表示，
 * 并且可以通过序列化和反序列化过程在网络或本地存储中传输和重建。
 */
@Data
@ToString
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    // 数据列表
    private List<T> items;

    // 总记录数
    private long counts;

    // 当前页面
    private long page;

    // 每页记录数
    private long pageSize;



}
