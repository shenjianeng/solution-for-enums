package com.github.shen.swagger.plugin;

import com.google.common.base.Joiner;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import springfox.documentation.builders.OperationBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.service.AllowableListValues;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.ParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spi.service.contexts.ParameterContext;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author shenjianeng
 * @date 2020/4/19
 */
@SuppressWarnings(value = "all")
public class EnumParameterBuilderPlugin implements ParameterBuilderPlugin, OperationBuilderPlugin {

	private static final Joiner joiner = Joiner.on(",");

	@Override
	public void apply(ParameterContext context) {
		Class<?> type = context.resolvedMethodParameter().getParameterType().getErasedType();
		if (Enum.class.isAssignableFrom(type)) {
			SwaggerDisplayEnum annotation = AnnotationUtils.findAnnotation(type, SwaggerDisplayEnum.class);
			if (annotation != null) {

				String index = annotation.index();
				String name = annotation.name();
				Object[] enumConstants = type.getEnumConstants();
				List<String> displayValues = Arrays.stream(enumConstants).filter(Objects::nonNull).map(item -> {
					Class<?> currentClass = item.getClass();

					Field indexField = ReflectionUtils.findField(currentClass, index);
					ReflectionUtils.makeAccessible(indexField);
					Object value = ReflectionUtils.getField(indexField, item);

					Field descField = ReflectionUtils.findField(currentClass, name);
					ReflectionUtils.makeAccessible(descField);
					Object desc = ReflectionUtils.getField(descField, item);
					return value.toString();

				}).collect(Collectors.toList());

				ParameterBuilder parameterBuilder = context.parameterBuilder();
				AllowableListValues values = new AllowableListValues(displayValues, "LIST");
				parameterBuilder.allowableValues(values);
			}
		}
	}


	@Override
	public boolean supports(DocumentationType delimiter) {
		return true;
	}

	@Override
	public void apply(OperationContext context) {
		OperationBuilder operationBuilder = context.operationBuilder();
		Field parametersField = ReflectionUtils.findField(operationBuilder.getClass(), "parameters");
		ReflectionUtils.makeAccessible(parametersField);
		List<Parameter> parameters = (List<Parameter>) ReflectionUtils.getField(parametersField, operationBuilder);
		if (null == parameters) {
			return;
		}

		parameters.forEach(parameter -> {
			if (parameter.getType().isPresent()) {
				Class<?> clazz = parameter.getType().get().getErasedType();
				if (Enum.class.isAssignableFrom(clazz)) {
					SwaggerDisplayEnum annotation = AnnotationUtils.findAnnotation(clazz, SwaggerDisplayEnum.class);
					if (annotation != null) {
						String index = annotation.index();
						String name = annotation.name();
						Object[] enumConstants = clazz.getEnumConstants();

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

						List<String> allowValues = Arrays.stream(enumConstants).filter(Objects::nonNull).map(item -> {
							Class<?> currentClass = item.getClass();

							Field indexField = ReflectionUtils.findField(currentClass, index);
							ReflectionUtils.makeAccessible(indexField);
							Object value = ReflectionUtils.getField(indexField, item);
							return String.valueOf(value);

						}).collect(Collectors.toList());

						final AllowableListValues allowableListValues = new AllowableListValues(allowValues, clazz.getTypeName());

						Field description = ReflectionUtils.findField(parameter.getClass(), "description");
						Field allowableValues = ReflectionUtils.findField(parameter.getClass(), "allowableValues");
						ReflectionUtils.makeAccessible(description);
						ReflectionUtils.makeAccessible(allowableValues);
						Object field = ReflectionUtils.getField(description, parameter);
						ReflectionUtils.setField(description, parameter, field + " , " + joiner.join(displayValues));
						ReflectionUtils.setField(allowableValues, parameter, allowableListValues);
					}
				}
			}
		});
	}
}
