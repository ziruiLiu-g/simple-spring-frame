# Simple-Spring-Frame

Just for learning purpose

I make a simple version of spring-framework in this project. This framework contains the simple version of @Autowired, @Requestmapping, @Controller, @RequestParam and @Service annotations, which are frequently used in the real spring-framework



# The structure of the project

### com.lzr.simple.spring.framework.annotation

In this document, I create 5 new annotation files which represent the frequently used annotation in spring-framework



### com.lzr.simple.spring.framework.config

I this document, the MyConfig class is mainly used to store the configuration file("application.properties")



### com.lzr.simple.spring.framework.handler

The HandlerMapping class is mainly used to store the request url and the match method which will be called when users access the url. The arguements of the method will also be stored in the HandlerMapping Object.

In this class, all the fields will be gotten by reflection.



### com.lzr.simple.spring.framework.SimpleDispatcherServlet.java

This class is the most important part of simple-spring-frame.

You can find out the whole process that shows how spring-framework loads the configuration, scans the packages, initialize objects and injects the dependency.

The main steps are as below:

- Step1:	load the configuration file and store the configs in the MyConfig object; (doLoadConfig())

- Step2:    scan the basePath which has been gotten from application.properties, and get all .class files under this path; (doScanPacakge())

- Step3:    check whether all these classes just got by step2 have the spring annotations. If true, new an object for the class and store it in the iocContainerMap; (doInitializedClass())

- Step4:    check the object in iocContainerMap one by one. If the fields in the object decorated by the @Autowired annotation, just get the appropriate instance from the iocContainerMap and set this instance to the fields.

  For Example:

  We have the code below:

  ```
  @CustomAutowired
  private SimpleServer simpleServer;
  ```

  The SimpleServer is defined like this:

  ```
  @CustomService(value = "SimpleServer")
  public class SimpleServer {
  }
  ```

  Then, we will find out the instance whose key is "SimpleServer" from iocContainerMap, and set this intance to the field which decorated by the @CustomAutowired

  

  After  initializing the fields, the framework will check whether the class contains @CustomController annotation. If true, get the base Url that represent this controller(the base Url can be found in the field 'value' defined in the annotation. If the users do not set this value, the base Url will be "/")

  The next process is to check whether the methods in this class have @CustomController and check whether the arguments. The arguments that decorated by @CustomRequestParam will be collected and stored in the handlerMapping. When the users access the url, the framework will check whether the information that sent by the users can match the arguments' name.

  For Example:

  Now we have a class decorated by @CustomController

  ```java
  @CustomController
  public class SimpleController {
      ....
  }
  ```

  And there is a method "query" decorated by @CustomGetMapping in this class

  ```java
  @CustomGetMapping("/simple")
  public void query(HttpServletRequest request, HttpServletResponse response,
                    @CustomRequestParam("name") String name) throws IOException {
      response.setContentType("text/html;charset=utf-8");
      
      response.getWriter().write("[simple-spring-frame] name: " + name);
  }
  ```

  So now, the framework will know the url of this api is "/simple", and the arguments of this method is "name"

  

  So, when the user enter this url in browser: http://localhost:8080/simple?name=123, the framework will turn to handlerMappingMap, and find out HandlerMapping whose key is "localhost:8080/simple". After getting the appropriate HandlerMapping, the framework will find out whether the methodParams contains key "name". If true, the value "123" will be sent to the api;





## Tips

Before using the servlet to build your own spring framework, you should add the file ***src/main/webapp/WEB-INF/web.xml***. The HttpServlet can find out your dispatcherServlet only if you appoint the class in the web.xml file.

