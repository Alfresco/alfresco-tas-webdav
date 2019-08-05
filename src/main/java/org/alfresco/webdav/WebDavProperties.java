package org.alfresco.webdav;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@PropertySource("classpath:default.properties")
@PropertySource(value = "classpath:${environment}.properties", ignoreResourceNotFound = true)
public class WebDavProperties
{

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer()
    {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Value("${webdav.port}")
    private int webdavPort;

    public int getWebDavPort()
    {
        return webdavPort;
    }

    public void setWebDavPort(int webDavPort)
    {
        this.webdavPort = webDavPort;
    }
}
