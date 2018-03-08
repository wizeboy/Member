package com.xinqushi.interceptor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class MyWebMvcConfigurer extends WebMvcConfigurerAdapter {
	
	 //关键，将拦截器作为bean写入配置中
    @Bean
    public MyInterceptor myInterceptor(){
        return new MyInterceptor();
    }
	
    /**
     * 
     * <p>Title: addInterceptors</p>   
     * <p>Description: addPathPatterns：拦截路径 </p>   
     * @param registry   
     * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter#addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry)
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(myInterceptor())
        	.addPathPatterns("/member/**")
        	.excludePathPatterns("/error")
        	.excludePathPatterns("/member/login");
        super.addInterceptors(registry);
    }
     
    /**
     * 
     * <p>Title: addResourceHandlers</p>   
     * <p>Description: 静态资源路径配置</p>   
     * @param registry   
     * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter#addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry)
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    	registry.addResourceHandler("/member/assets/**").addResourceLocations("classpath:/static/assets/");
    	registry.addResourceHandler("/member/javascript/**").addResourceLocations("classpath:/static/javascript/");
    	registry.addResourceHandler("/member/quill/**").addResourceLocations("classpath:/static/quill/");
    	registry.addResourceHandler("/member/layer/**").addResourceLocations("classpath:/static/layer/");
    	registry.addResourceHandler("/member/**").addResourceLocations("classpath:/static/");
        super.addResourceHandlers(registry);
    }
}
