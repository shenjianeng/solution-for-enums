package com.github.shen.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.shen.mvc.plugin.EnumConvertMethod;
import com.github.shen.swagger.plugin.SwaggerDisplayEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 课程类型,102:图文,103:音频,104:视频,105:外链
 */
@Getter
@AllArgsConstructor
@SwaggerDisplayEnum(index = "type", name = "desc")
public enum CourseType {

    /**
     * 图文
     */
    PICTURE(102, "图文"),
    /**
     * 音频
     */
    AUDIO(103, "音频"),
    /**
     * 视频
     */
    VIDEO(104, "视频"),
    /**
     * 外链
     */
    URL(105, "外链"),
    ;

    @JsonValue
    private final int type;
    private final String desc;

    private static final Map<Integer, CourseType> mappings;

    static {
        Map<Integer, CourseType> temp = new HashMap<>();
        for (CourseType courseType : values()) {
            temp.put(courseType.type, courseType);
        }
        mappings = Collections.unmodifiableMap(temp);
    }

    @EnumConvertMethod
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    @Nullable
    public static CourseType resolve(int index) {
        return mappings.get(index);
    }

}
