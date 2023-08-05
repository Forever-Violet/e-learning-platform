package com.xuecheng.content.api;

import com.alibaba.fastjson.JSONObject;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description 课程计划编辑接口
 * @author itcast
 */
@Api(value = "课程计划编辑接口", tags = "课程计划编辑接口")
@RestController
public class TeachplanController {

    @Autowired
    private TeachplanService  teachplanService;


    @ApiOperation("查询课程计划树形结构")
    @ApiImplicitParam(value = "courseId", name = "课程Id", required = true)
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachPlanDto> getTreeNodes(@PathVariable Long courseId) {
        return teachplanService.findTeachplanTree(courseId);

    }

    @ApiOperation("课程计划创建或修改")
    @PostMapping("/teachplan")
    public void saveTeachplan(@RequestBody SaveTeachplanDto teachplan) {
        teachplanService.saveTeachplan(teachplan);
    }

    @ApiOperation("课程计划删除")
    @ApiImplicitParam(value = "teachplanId", name = "课程计划Id", required = true)
    @DeleteMapping("/teachplan/{id}")
    public ResponseEntity<?> deleteTeachplan(@PathVariable("id") Long teachplanId) {

        try {
            teachplanService.deleteTeachplan(teachplanId);
        } catch (XueChengPlusException e) { //如果捕获到因大章节还有子节点而删除失败的异常
            JSONObject responseJson = new JSONObject();
            responseJson.put("errCode", e.getErrCode());
            responseJson.put("errMessage", e.getErrMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseJson);
        }
        return ResponseEntity.ok().build();
    }


    @ApiOperation("下移课程计划")
    @PostMapping("/teachplan/movedown/{id}")
    @ApiImplicitParam(value = "teachplanId", name = "课程计划Id", required = true)
    public void moveDownTeachplan(@PathVariable("id") Long teachplanId) {
        teachplanService.moveDownTeachplan(teachplanId);
    }

    @ApiOperation("上移课程计划")
    @PostMapping("/teachplan/moveup/{id}")
    @ApiImplicitParam(value = "teachplanId", name = "课程计划Id", required = true)
    public void moveUpTeachplan(@PathVariable("id") Long teachplanId) {
        teachplanService.moveUpTeachplan(teachplanId);
    }

    @ApiOperation(value = "课程计划和媒资文件绑定")
    @PostMapping("/teachplan/association/media")
    public void associationMedia(@RequestBody BindTeachplanMediaDto bindTeachplanMediaDto) {
        teachplanService.associationMedia(bindTeachplanMediaDto);
    }

    @ApiOperation(value = "解除课程计划与媒资的绑定")
    @DeleteMapping("/teachplan/association/media/{teachPlanId}/{mediaId}")
    public void unBindAssociationMedia(@PathVariable("teachPlanId") Long teachPlanId,
                                       @PathVariable("mediaId") String mediaId) {
        teachplanService.unBindAssociationMedia(teachPlanId, mediaId);
    }

}
