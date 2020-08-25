package io.github.tonybro233.littlewheels.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class RequestBodyParamConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestBodyParamConfiguration.class);

    @Bean
    public RequestBodyParamWebMvcConfigurer requestBodyParamWebMvcConfigurer(
            ObjectMapper objectMapper, BeanFactory beanFactory) {
        return new RequestBodyParamWebMvcConfigurer(objectMapper, beanFactory);
    }

    static class RequestBodyParamWebMvcConfigurer implements WebMvcConfigurer {

        private final ObjectMapper objectMapper;

        private final BeanFactory beanFactory;

        public RequestBodyParamWebMvcConfigurer(ObjectMapper objectMapper, BeanFactory beanFactory) {
            this.objectMapper = objectMapper;
            this.beanFactory = beanFactory;
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            ConfigurableBeanFactory configurableBeanFactory = null;
            if (beanFactory instanceof ConfigurableBeanFactory) {
                configurableBeanFactory = (ConfigurableBeanFactory) beanFactory;
            }
            if (null == configurableBeanFactory) {
                LOGGER.warn("Cannot get Configurable Bean Factory");
            }
            resolvers.add(new RequestBodyParamArgumentResolver(objectMapper, configurableBeanFactory));
        }

    }

}
