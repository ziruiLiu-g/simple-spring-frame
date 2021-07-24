package com.lzr.simple.spring.framework.config;

import lombok.Data;

@Data
public class MyConfig {
    private String basePackages;
    
    public String getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(String basePackages) {
        this.basePackages = basePackages;
    }
}
