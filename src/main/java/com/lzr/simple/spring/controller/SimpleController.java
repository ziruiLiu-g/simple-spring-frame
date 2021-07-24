package com.lzr.simple.spring.controller;

import com.lzr.simple.spring.framework.annotation.CustomAutowired;
import com.lzr.simple.spring.framework.annotation.CustomController;
import com.lzr.simple.spring.framework.annotation.CustomGetMapping;
import com.lzr.simple.spring.framework.annotation.CustomRequestParam;
import com.lzr.simple.spring.service.SimpleServer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@CustomController
public class SimpleController {
    @CustomAutowired
    private SimpleServer simpleServer;
    
    @CustomGetMapping("/simple")
    public void query(HttpServletRequest request, HttpServletResponse response,
                      @CustomRequestParam("name") String name) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        
        response.getWriter().write("[simple-spring-frame] name: " + name);
    }
}
