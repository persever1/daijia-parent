package com.atguigu.daijia.common.config.knife4j;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    /**
     * 配置Swagger的分组接口。
     * 该方法创建一个名为“web-api”的分组，包含所有API路径。
     * @return GroupedOpenApi 对象，定义了API的分组和包含的路径。
     */
    @Bean
    public GroupedOpenApi webApi() {
        // 构建一个分组，命名为“web-api”，匹配所有路径
        return GroupedOpenApi.builder()
                .group("web-api")
                .pathsToMatch("/**")
                .build();
    }

//    @Bean
//    public GroupedOpenApi adminApi() {
//        return GroupedOpenApi.builder()
//                .group("admin-api")
//                .pathsToMatch("/admin/**")
//                .build();
//    }

    /**
     * 配置自定义的OpenAPI信息。
     *
     * 通过此方法配置的OpenAPI信息将用于文档和接口的展示。它定义了API的基本元数据，
     * 如API的标题、版本和描述，以及联系人信息。
     *
     * @return OpenAPI 对象，包含了API的元数据信息。
     */
    @Bean
    public OpenAPI customOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("代驾API接口文档")
                        .version("1.0")
                        .description("代驾API接口文档")
                        .contact(new Contact().name("qy")));
    }



}
