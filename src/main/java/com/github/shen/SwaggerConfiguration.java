package com.github.shen;

import com.github.shen.swagger.plugin.EnumModelPropertyBuilderPlugin;
import com.github.shen.swagger.plugin.EnumParameterBuilderPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author shenjianeng
 * @date 2020/5/10
 */
@EnableSwagger2
@Configuration
public class SwaggerConfiguration {

    @Bean
    public EnumModelPropertyBuilderPlugin enumModelPropertyBuilderPlugin() {
        return new EnumModelPropertyBuilderPlugin();
    }


    @Bean
    public EnumParameterBuilderPlugin enumParameterBuilderPlugin() {
        return new EnumParameterBuilderPlugin();
    }
}
