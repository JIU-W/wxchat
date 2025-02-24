package com.wxchat;

import com.wxchat.entity.config.AppConfig;
import com.wxchat.entity.constants.Constants;
import com.wxchat.spring.ApplicationContextProvider;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.servlet.MultipartConfigElement;

@EnableAsync
@SpringBootApplication(scanBasePackages = {"com.wxchat"})
@MapperScan(basePackages = {"com.wxchat.mappers"})
@EnableTransactionManagement
@EnableScheduling
public class WxChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(WxChatApplication.class, args);
    }

    /**
     * 文件上传临时路径
     * @return
     */
    @Bean
    @DependsOn({"applicationContextProvider"})
    MultipartConfigElement multipartConfigElement() {
        //获取名为appConfig的 Bean
        AppConfig appConfig = (AppConfig) ApplicationContextProvider.getBean("appConfig");
        //MultipartConfigFactory 是Spring用于配置 multipart 文件上传的工厂类
        MultipartConfigFactory factory = new MultipartConfigFactory();
        //设置临时文件存储路径
        factory.setLocation(appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP);
        //创建并返回 MultipartConfigElement 对象
        //用于支持文件上传功能（例如设置最大文件大小、临时存储路径等）
        return factory.createMultipartConfig();
    }

}
