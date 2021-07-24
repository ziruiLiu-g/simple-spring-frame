package com.lzr.simple.spring.framework;

import com.lzr.simple.spring.framework.annotation.*;
import com.lzr.simple.spring.framework.config.MyConfig;
import com.lzr.simple.spring.framework.handler.HandlerMapping;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class SimpleDispatcherServlet extends HttpServlet {
    private MyConfig myConfig = new MyConfig();
    
    private List<String> classNameList = new ArrayList<>();

    private Map<String, Object> iocContainerMap = new HashMap<>();
    
    private Map<String, HandlerMapping> handlerMappingMap = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        // step1: load the config file
        try {
            doLoadConfig(config.getInitParameter("defaultConfig"));
        } catch (Exception e) {
            System.out.println("can not get config file");
            return;
        }
        
        // step2: scan the path(get from config file)
        doScanPacakge(myConfig.getBasePackages());
        
        // step3: init the classes scanned out, put them into ioc container
        doInitializedClass();
        
        // step4: Dependency Injection
        doDependencyInjection();

        System.out.println("DispatchServlet Initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            this.doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUrl = this.formatUrl(request.getRequestURI());
        HandlerMapping handlerMapping = handlerMappingMap.get(requestUrl);
        
        if (null == handlerMapping) {
            response.getWriter().write("404 not found!");
            return;
        }
        
        Class<?>[] paramTypeArr = handlerMapping.getMethod().getParameterTypes();
        Object[] paramArr = new Object[paramTypeArr.length];
        
        for (int i = 0; i < paramTypeArr.length; i++) {
            Class<?> clazz = paramTypeArr[i];
            
            // here we just use three arg type to demostrate the project
            if (clazz == HttpServletRequest.class) {
                paramArr[i] = request;
            } else if (clazz == HttpServletResponse.class) {
                paramArr[i] = response;
            } else if (clazz == String.class) {
                Map<Integer, String> methodParam = handlerMapping.getMethodParams();
                paramArr[i] = request.getParameter(methodParam.get(i));
            } else {
                System.out.println("param type not support yet");
            }
        }
        
        // use reflection to run the method
        handlerMapping.getMethod().invoke(handlerMapping.getTarget(), paramArr);
    }

    /**
     * parse the uri to url
     * 
     * @param requestUrl requestUrl
     * @return final url
     */
    private String formatUrl(String requestUrl) {
        requestUrl = requestUrl.replaceAll("/+", "/");
        if (requestUrl.lastIndexOf("/") == requestUrl.length() - 1) {
            requestUrl = requestUrl.substring(0, requestUrl.length() - 1);
        }
        return requestUrl;
    }

    /**
     * load configuration
     * 
     * @param configPath configPath
     */
    private void doLoadConfig(String configPath) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(configPath);
        Properties properties = new Properties();
        
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("load configuration fail");
        }
        
        properties.forEach((k, v) -> {
            try {
                Field[] declaredFields = myConfig.getClass().getDeclaredFields();
                for (Field f : declaredFields) {
                    System.out.println(f.getName());
                }
                
                Field field = myConfig.getClass().getDeclaredField((String) k);
                field.setAccessible(true);
                field.set(myConfig, v);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("init configuration failed");
                return;
            }
        });
    }

    /**
     * scan pacakge and get all classes' name
     * 
     * @param basePackages basePackages
     */
    private void doScanPacakge(String basePackages) {
        if (StringUtils.isBlank(basePackages)) {
            return;
        }

        // switch the '.' to '/'
        String scanPath = "/" + basePackages.replaceAll("\\.", "/");
        
        // scan the whole path of current package
        URL url = this.getClass().getClassLoader().getResource(scanPath);
        
        // get all files under the path
        File files = new File(url.getFile());
        
        // scan
        for (File file : files.listFiles()) {
            // scan file
            // if the path is dir, scan deeper recursively
            // if not dir, add to the classNameList
            if (file.isDirectory()) {
                doScanPacakge(basePackages + "." + file.getName());
            } else {
                classNameList.add(basePackages + "." + file.getName().replace(".class", ""));
            }
        }
    }

    /**
     * init classes, and put them into container
     */
    private void doInitializedClass() {
        if (classNameList.isEmpty()) {
            return;
        }
        
        for (String className : classNameList) {
            if (StringUtils.isEmpty(className)) {
                continue;
            }
            
            Class clazz;
            try {
                // get object by reflection
                clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(CustomController.class)) {
                    String value = ((CustomController) clazz.getAnnotation(CustomController.class)).value();
                    
                    // if the annotation set value, then get value
                    // if not, get the class name(first char lowercase) as the value
                    iocContainerMap.put(StringUtils.isBlank(value) ?
                            toLowerFirstLetterCase(clazz.getSimpleName()) : value, clazz.newInstance());
                } else if (clazz.isAnnotationPresent(CustomService.class)) {
                    String value = ((CustomService) clazz.getAnnotation(CustomService.class)).value();
                    iocContainerMap.put(StringUtils.isBlank(value) ?
                            toLowerFirstLetterCase(clazz.getSimpleName()) : value, clazz.newInstance());
                } else {
                    System.out.println("not support other annotation yet");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("init class failed: " + className);
            }
        }
    }
    
    private void doDependencyInjection() {
        if (iocContainerMap.size() == 0) {
            return;
        }
        
        Iterator<Map.Entry<String, Object>> iterator = iocContainerMap.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            Class<?> clazz = entry.getValue().getClass();
            Field[] fields = clazz.getDeclaredFields();
            
            for (Field field : fields) {
                // if have customAutowired, then injection
                if (field.isAnnotationPresent(CustomAutowired.class)) {
                    String value = toLowerFirstLetterCase(field.getType().getSimpleName());
                    
                    if (field.getType().isAnnotationPresent(CustomService.class)) {
                        CustomService cService = field.getType().getAnnotation(CustomService.class);
                        value = cService.value();
                    }
                    field.setAccessible(true);
                    
                    try {
                        Object target = iocContainerMap.get(value);
                        if (null == target) {
                            System.out.println(clazz.getName() + "required bean:" + value + ",but we not found it");
                        }
                        
                        field.set(entry.getValue(), iocContainerMap.get(value));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            // init handlerMapping
            String requestUrl = "";
            
            // get controller class url
            if (clazz.isAnnotationPresent(CustomController.class)) {
                requestUrl = clazz.getAnnotation(CustomController.class).value();
            }

            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(CustomGetMapping.class)) {
                    continue;
                }

                CustomGetMapping customGetMapping = method.getDeclaredAnnotation(CustomGetMapping.class);
                requestUrl = requestUrl + customGetMapping.value();
                
                // here not support the /*
                if (handlerMappingMap.containsKey(requestUrl)) {
                    System.out.println("url already exists");
                    continue;
                }
                
                // get methods' params annotation
                Annotation[][] annotationArr = method.getParameterAnnotations();
                
                Map<Integer, String> methodParam = new HashMap<>();
                retryParam:
                for (int i = 0; i < annotationArr.length; i++) {
                    for (Annotation annotation : annotationArr[i]) {
                        if (annotation instanceof CustomRequestParam) {
                            CustomRequestParam cRequestParam = (CustomRequestParam) annotation;
                            methodParam.put(i, cRequestParam.value());
                            continue retryParam;
                        }
                    }
                }
                
                requestUrl = this.formatUrl(requestUrl);
                HandlerMapping handlerMapping = new HandlerMapping();
                handlerMapping.setRequestUrl(requestUrl);
                handlerMapping.setMethod(method);
                handlerMapping.setTarget(entry.getValue());
                handlerMapping.setMethodParams(methodParam);
                handlerMappingMap.put(requestUrl, handlerMapping);
            }
        }
    }

    /**
     * toLowerFirstLetterCase
     *
     * @param className className
     * @return final result
     */
    private String toLowerFirstLetterCase(String className) {
        if (StringUtils.isBlank(className)) {
            return "";
        }
        String firstLetter = className.substring(0, 1);
        return firstLetter.toLowerCase() + className.substring(1);
    }

}
