package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.service.CourseBaseService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 课程发布 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class CoursePublishServiceImpl extends ServiceImpl<CoursePublishMapper, CoursePublish> implements CoursePublishService {

    @Autowired
    CourseBaseService courseBaseService;

    @Autowired
    TeachplanService teachplanService;

    @Autowired
    CoursePublishMapper coursePublishMapper;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CoursePublishPreMapper coursePublishPreMapper;

    @Autowired
    MqMessageService mqMessageService;

    @Autowired
    MediaServiceClient mediaServiceClient;


    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {

        // 课程基本信息、营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseService.getCourseBaseInfoById(courseId);

        // 课程计划信息
        List<TeachPlanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);

        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfo);
        coursePreviewDto.setTeachplans(teachplanTree);

        return coursePreviewDto;
    }

    @Transactional
    @Override
    public void commitAudit(Long companyId, Long courseId) {
        // 约束校验
        CourseBase courseBase = courseBaseService.getById(courseId);
        // 课程审核状态
        String auditStatus = courseBase.getAuditStatus();
        // 当审核状态为已提交时, 不允许再次提交审核
        if ("202003".equals(auditStatus)) {
            XueChengPlusException.cast("当前为等待审核状态，审核完成后可再次提交审核。");
        }
        // 本机构只允许提交本机构的课程
        if (!courseBase.getCompanyId().equals(companyId)) {
            XueChengPlusException.cast("不允许提交其他机构的课程。");
        }
        // 课程图片是否填写
        if (StringUtils.isEmpty(courseBase.getPic())) {
            XueChengPlusException.cast("提交失败，请上传课程图片。");
        }

        // 添加课程预发布记录到 预发布记录表
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        // 课程基本信息加部分营销信息
        CourseBaseInfoDto courseBaseInfoDto = courseBaseService.getCourseBaseInfoById(courseId);
        // copy数据
        BeanUtils.copyProperties(courseBaseInfoDto, coursePublishPre);
        // 课程营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        // 转为json
        String courseMarketJson = JSON.toJSONString(courseMarket);
        // 将课程营销信息json数据放入课程预发布表
        coursePublishPre.setMarket(courseMarketJson);

        // 查询课程计划信息
        List<TeachPlanDto> teachPlanTree = teachplanService.findTeachplanTree(courseId);
        if (teachPlanTree.size() == 0) {
            XueChengPlusException.cast("提交失败，请添加课程大纲。");
        }
        // 转为json数据存入预发布表中
        String teachPlanTreeJson = JSON.toJSONString(teachPlanTree);
        coursePublishPre.setTeachplan(teachPlanTreeJson);

        // 设置预发布记录状态, 已提交
        coursePublishPre.setStatus("202003");
        // 教学机构id
        coursePublishPre.setCompanyId(companyId);
        // 提交时间
        coursePublishPre.setCreateDate(LocalDateTime.now());
        // 先检查预发布表中有无此课程的预发布记录
        CoursePublishPre coursePublishPreUpdate = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPreUpdate == null) {
            // 如果没有, 则添加课程预发布记录
            coursePublishPreMapper.insert(coursePublishPre);
        } else {
            // 若存在, 则更新
            coursePublishPreMapper.updateById(coursePublishPre);
        }

        // 更新课程基本表的审核状态为 已提交
        courseBase.setAuditStatus("202003");
        courseBaseService.updateById(courseBase);
    }


    @Transactional //本地事务
    @Override
    public void publish(Long companyId, Long courseId) {
        // 约束校验
        // 查询课程预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null) {
            XueChengPlusException.cast("请先提交课程审核，审核通过后才可以发布。");
        }
        // 本机构只允许发布本机构的课程
        if (!coursePublishPre.getCompanyId().equals(companyId)) {
            XueChengPlusException.cast("不允许发布其他机构的课程。");
        }

        // 课程审核状态校验
        String auditStatus = coursePublishPre.getStatus();
        if (!"202004".equals(auditStatus)) { // 课程审核状态是否为通过
            XueChengPlusException.cast("操作失败，课程审核未通过。");
        }

        // 保存课程发布信息
        saveCoursePublish(courseId);

        // 保存发布到消息表, 以便后续调度任务完成分布式事务 实现A, P, 最终一致性
        saveCoursePublishMessage(courseId);

        // 删除课程预发布表对应的记录
        coursePublishPreMapper.deleteById(courseId);

    }

    /**
     * @description 保存课程发布信息到课程(已)发布表
     * @param courseId 课程id
     */
    private void saveCoursePublish(Long courseId) {
        // 整合课程发布信息
        // 查询课程预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null) {
            XueChengPlusException.cast("课程发布数据为空。");
        }

        CoursePublish coursePublish = new CoursePublish();

        // 拷贝到课程发布对象
        BeanUtils.copyProperties(coursePublishPre, coursePublish);
        // 设置状态为 已发布
        coursePublish.setStatus("203002");
        // 检查在课程发布表中已经存在当前课程
        CoursePublish coursePublishUpdate = coursePublishMapper.selectById(courseId);
        if (coursePublishUpdate == null) { // 若不存在, 则插入
            coursePublishMapper.insert(coursePublish);
        } else { // 若存在, 增更新
            coursePublishMapper.updateById(coursePublish);
        }

        // 更新课程基本表的发布状态
        CourseBase courseBase = courseBaseService.getById(courseId);
        // 设置状态为 已发布
        courseBase.setStatus("203002");
        courseBaseService.updateById(courseBase);

    }

    /**
     * @description 保存消息到消息表
     * @param courseId 课程id
     */
    private void saveCoursePublishMessage(Long courseId) {

        MqMessage mqMessage = mqMessageService.addMessage("course_publish",
                String.valueOf(courseId), null, null);
        if (mqMessage == null) { //如果插入失败
            XueChengPlusException.cast(CommonError.UNKNOWN_ERROR);
        }

    }

    @Override
    public File generateCourseHtml(Long courseId) {

        // 静态化文件
        File htmlFile = null;

        try{
            // 配置freemarker
            Configuration configuration = new Configuration(Configuration.getVersion());

            // 加载模板
            // 选定指定模板路径, classpath下templates下
            // 得到classpath路径
            String classpath = this.getClass().getResource("/").getPath();
            configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));
            // 设定字符编码
            configuration.setDefaultEncoding("utf-8");

            // 指定模板文件名称
            Template template = configuration.getTemplate("course_template.ftl");

            // 准备数据
            CoursePreviewDto coursePreviewDto = this.getCoursePreviewInfo(courseId);

            Map<String, Object> map = new HashMap<>();
            map.put("model", coursePreviewDto); //因为要给数据指定名称"model", 所以使用map
            // 静态化
            // 参数1: 模板, 参数2: 数据模型
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
            // 将静态化内容输出到文件(输入流)中
            InputStream inputStream = IOUtils.toInputStream(content);
            // 创建新的(空的、临时的)静态化文件
            htmlFile = File.createTempFile("course", ".html");
            log.debug("课程静态化, 生成静态文件:{}", htmlFile.getAbsolutePath());
            // 输出六
            FileOutputStream outputStream = new FileOutputStream(htmlFile);
            // 将输入流的数据拷贝到输出流
            IOUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            log.error("课程静态化出现异常:{}", e.toString());
            XueChengPlusException.cast("课程静态化异常");
        }

        return htmlFile;
    }

    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        // 将File类型文件转为MultipartFile类型, 以便传输
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        try {
            String course = mediaServiceClient.upload(multipartFile, "course/" + courseId + ".html");
            if (course == null) {
                XueChengPlusException.cast("上传静态文件异常");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
