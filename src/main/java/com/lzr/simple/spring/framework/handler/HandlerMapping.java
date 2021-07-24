package com.lzr.simple.spring.framework.handler;

import lombok.Data;

import java.lang.reflect.Method;
import java.util.Map;

@Data
public class HandlerMapping {
    private String requestUrl;
    
    private Object target;
    
    private Method method;
    
    private Map<Integer,String> methodParams;
}
