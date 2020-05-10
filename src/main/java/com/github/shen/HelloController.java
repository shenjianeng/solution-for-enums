package com.github.shen;

import com.github.shen.enums.CourseType;
import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

/**
 * @author shenjianeng
 * @date 2020/5/10
 */
@Validated
@RestController
public class HelloController {


    @ApiOperation(value = "测试swagger自动列举枚举值")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "courseType", value = "课程类型", dataType = "int", required = true),
    })
    @GetMapping("/hello")
    public Rt getDownloadCourse(@NotNull @RequestParam CourseType courseType) {
        return new Rt(courseType);
    }

    @AllArgsConstructor
    @Getter
    @ApiModel(description = "返回结果")
    static class Rt {
        @ApiModelProperty(value = "课程类型", required = true, example = "101")
        private final CourseType courseType;
    }
}

