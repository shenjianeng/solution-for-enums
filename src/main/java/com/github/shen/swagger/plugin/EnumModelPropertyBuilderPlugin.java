package com.github.shen.swagger.plugin;

import com.google.common.base.Optional;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import springfox.documentation.builders.ModelPropertyBuilder;
import springfox.documentation.schema.Annotations;
import springfox.documentation.service.AllowableListValues;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.ModelPropertyBuilderPlugin;
import springfox.documentation.spi.schema.contexts.ModelPropertyContext;
import springfox.documentation.swagger.schema.ApiModelProperties;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author shenjianeng
 * @date 2020/4/15
 */
@SuppressWarnings(value = "all")
public class EnumModelPropertyBuilderPlugin implements ModelPropertyBuilderPlugin {

	@Override
	public void apply(ModelPropertyContext context) {
		Optional<ApiModelProperty> annotation = Optional.absent();
		Class<?> rawPrimaryType = null;

		if (context.getAnnotatedElement().isPresent()) {
			annotation = annotation.or(ApiModelProperties.findApiModePropertyAnnotation(context.getAnnotatedElement().get()));
			rawPrimaryType = ((Field) context.getAnnotatedElement().get()).getType();
		}
		if (context.getBeanPropertyDefinition().isPresent()) {
			annotation = annotation.or(Annotations.findPropertyAnnotation(
					context.getBeanPropertyDefinition().get(),
					ApiModelProperty.class));
			rawPrimaryType = context.getBeanPropertyDefinition().get().getRawPrimaryType();
		}

		if (annotation.isPresent() && null != rawPrimaryType) {
			addDescForEnum(context, rawPrimaryType);
		}
	}

	@Override
	public boolean supports(DocumentationType delimiter) {
		return true;
	}

	private void addDescForEnum(ModelPropertyContext context, Class<?> fieldType) {
		if (Enum.class.isAssignableFrom(fieldType)) {
			SwaggerDisplayEnum annotation = AnnotationUtils.findAnnotation(fieldType, SwaggerDisplayEnum.class);
			if (annotation != null) {
				String index = annotation.index();
				String name = annotation.name();

				Object[] enumConstants = fieldType.getEnumConstants();

				List<String> displayValues = Arrays.stream(enumConstants).filter(Objects::nonNull).map(item -> {
					Class<?> currentClass = item.getClass();

					Field indexField = ReflectionUtils.findField(currentClass, index);
					ReflectionUtils.makeAccessible(indexField);
					Object value = ReflectionUtils.getField(indexField, item);

					Field descField = ReflectionUtils.findField(currentClass, name);
					ReflectionUtils.makeAccessible(descField);
					Object desc = ReflectionUtils.getField(descField, item);
					return value + ":" + desc;

				}).collect(Collectors.toList());

				List<String> allowableValues = Arrays.stream(enumConstants).filter(Objects::nonNull).map(item -> {
					Class<?> currentClass = item.getClass();

					Field indexField = ReflectionUtils.findField(currentClass, index);
					ReflectionUtils.makeAccessible(indexField);
					Object value = ReflectionUtils.getField(indexField, item);
					return String.valueOf(value);

				}).collect(Collectors.toList());

				ModelPropertyBuilder builder = context.getBuilder();
				Field descField = ReflectionUtils.findField(builder.getClass(), "description");
				ReflectionUtils.makeAccessible(descField);
				String joinText = ReflectionUtils.getField(descField, builder)
						+ " (" + String.join("; ", displayValues) + ")";

				builder.description(joinText).type(context.getResolver().resolve(Integer.class));

				final AllowableListValues allowableListValues = new AllowableListValues(allowableValues, fieldType.getTypeName());
				builder.allowableValues(allowableListValues);
			}
		}

	}
}
