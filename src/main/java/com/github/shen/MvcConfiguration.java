package com.github.shen;

import com.github.shen.mvc.plugin.EnumMvcConverterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author shenjianeng
 * @date 2020/4/19
 */
@Configuration
public class MvcConfiguration implements WebMvcConfigurer {

    @Bean
    public EnumMvcConverterFactory enumMvcConverterFactory() {
        return new EnumMvcConverterFactory();
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // org.springframework.core.convert.support.GenericConversionService.ConvertersForPair.add
        // this.converters.addFirst(converter);
        // 所以我们自定义的会放在前面
        registry.addConverterFactory(enumMvcConverterFactory());
    }
}
