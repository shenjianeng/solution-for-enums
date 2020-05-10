package com.github.shen.swagger.plugin;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.common.base.Optional;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.ModelPropertyBuilderPlugin;
import springfox.documentation.spi.schema.contexts.ModelPropertyContext;

import java.lang.reflect.Field;

/**
 * @author shenjianeng
 * @date 2020/4/15
 */
@SuppressWarnings(value = "all")
public class LongToStringModelPropertyBuilderPlugin implements ModelPropertyBuilderPlugin {

    @Override
    public void apply(ModelPropertyContext context) {
        Optional<BeanPropertyDefinition> optional = context.getBeanPropertyDefinition();
        if (!optional.isPresent()) {
            return;
        }

        final Class<?> fieldType = optional.get().getField().getRawType();

        addDescForLongId(context, fieldType);
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return true;
    }


    private void addDescForLongId(ModelPropertyContext context, Class<?> fieldType) {
        String fieldName = context.getBeanPropertyDefinition().get().getName();
        Class<?> declaringClass = context.getBeanPropertyDefinition().get().getField().getDeclaringClass();
        Field field = ReflectionUtils.findField(declaringClass, fieldName);
        JsonSerialize annotation = AnnotationUtils.findAnnotation(field, JsonSerialize.class);
        if (annotation != null) {
            if (ToStringSerializer.class.equals(annotation.using())) {
                context.getBuilder().type(context.getResolver().resolve(String.class));
            }
        }
    }
}
