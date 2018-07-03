package com.honor.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.honor.servlet.annotation.DemoAutowired;
import com.honor.servlet.annotation.DemoController;
import com.honor.servlet.annotation.DemoRequestMapping;
import com.honor.servlet.annotation.DemoRequestParam;
import com.honor.servlet.annotation.DemoService;

public class DemoServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private Properties contextConfig = new Properties();

	private List<String> classNames = new ArrayList<>();

	// IOC容器(beanName,instance)
	private Map<String, Object> ioc = new HashMap<>();
	// handlerMapping(url,Method)
	//private Map<String, Method> handlerMapping = new HashMap<>();
	private List<Handler> handlerMapping = new ArrayList<>();
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 等待用户请求(请求分发)
		doDispatcher(req, resp);

	}

	@Override
	public void init(ServletConfig config) throws ServletException {

		// 1.加载配置文件
		String location = config.getInitParameter("contextConfigLocation");
		doLoadConfig(location);

		// 2.解析配置文件,读取scanPackage,扫描相关的类
		String property = contextConfig.getProperty("scanPackage");
		doScanner(property);

		// 3.实例化(刚刚扫描的类)
		doInstance();
		// 4.依赖注入
		doAutowired();

		// 5.初始化HandlerMapping
		initHandlerMapping();

		System.out.println("xxxxxxxxxxxxxxxx");
		System.out.println(contextConfig);
		System.out.println("xxxxxxxxxxxxxxxx");
		System.out.println(classNames);
		System.out.println("xxxxxxxxxxxxxxxx");
		System.out.println(ioc);
		System.out.println("xxxxxxxxxxxxxxxx");
		System.out.println(handlerMapping);
		System.out.println("xxxxxxxxxxxxxxxx");
	}

	private void initHandlerMapping() {
		if (ioc.isEmpty())	return;
		
		for (Entry<String, Object> entry : ioc.entrySet()) {
			Class<? extends Object> clazz = entry.getValue().getClass();

			if (!clazz.isAnnotationPresent(DemoController.class))	continue;

			String basePath = "";
			if (clazz.isAnnotationPresent(DemoRequestMapping.class)) {
				basePath = clazz.getAnnotation(DemoRequestMapping.class).value();
			}

			Method[] methods = clazz.getMethods();

			for (Method method : methods) {

				if (!method.isAnnotationPresent(DemoRequestMapping.class))
					continue;

				String path = method.getAnnotation(DemoRequestMapping.class).value();
				path = (basePath + "/" + path).replaceAll("/+", "/");
				
				Pattern pattern = Pattern.compile(path);
				
				handlerMapping.add(new Handler(entry.getValue(), method, pattern));
				
				//this.handlerMapping.put(path, method);
			}
		}

	}

	private void doLoadConfig(String contextConfigLocation) {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
		try {
			contextConfig.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void doScanner(String scanPackage) {
		// 骚操作
		String path = "/" + scanPackage.replace(".", "/");
		// String path = scanPackage.replace(".", File.separator);
		URL url = this.getClass().getResource(path);
		File classDir = new File(url.getFile());
		for (File file : classDir.listFiles()) {
			if (file.isDirectory()) {
				doScanner(scanPackage + "." + file.getName());
			} else {
				String className = scanPackage + "." + file.getName().replace(".class", "");
				classNames.add(className);
			}
		}
	}

	private void doInstance() {
		if (classNames.isEmpty())
			return;
		// ioc.put(beanName,instance)
		try {
			for (String className : classNames) {
				Class<?> clazz = Class.forName(className);

				// 只有加了注解的类进行初始化
				if (clazz.isAnnotationPresent(DemoController.class)) {
					Object instance = clazz.newInstance();
					// Spring中的beanId默认是类名的首字母小写
					String name = firstLowcase(clazz.getSimpleName());

					ioc.put(name, instance);
				} else if (clazz.isAnnotationPresent(DemoService.class)) {
					DemoService service = clazz.getAnnotation(DemoService.class);
					// 1.@Service自定义命名
					String beanName = service.value();
					// 2.默认命名
					if (beanName.trim().equals("")) {
						beanName = firstLowcase(clazz.getSimpleName());
					}
					Object instance = clazz.newInstance();
					ioc.put(beanName, instance);
					// 3.接口初始化
					Class<?>[] interfaces = clazz.getInterfaces();
					for (Class<?> i : interfaces) {
						ioc.put(i.getName(), instance);
					}

				} else {
					continue;
				}
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
		// TODO Auto-generated method stub

	}

	private void doAutowired() {
		if (ioc.isEmpty())
			return;

		try {
			// 赋值就是从ioc容器中取值
			for (Entry<String, Object> entry : ioc.entrySet()) {
				Field[] fields = entry.getValue().getClass().getDeclaredFields();
				for (Field field : fields) {

					if (!field.isAnnotationPresent(DemoAutowired.class))
						continue;

					DemoAutowired autowired = field.getAnnotation(DemoAutowired.class);
					String beanName = autowired.value().trim();
					if (beanName.equals("")) {
						beanName = field.getType().getName();
					}

					field.setAccessible(true);// 暴力访问
					field.set(entry.getValue(), ioc.get(beanName));
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

//	private void doDispatcher1(HttpServletRequest req, HttpServletResponse resp) throws IOException {
//
//		String url = req.getRequestURI();
//		String contextPath = req.getContextPath();
//		url = url.replace(contextPath, "").replaceAll("/+", "/");
//
//		if (!this.handlerMapping.containsKey(url)) {
//			resp.getWriter().write("Not Found 404 !!!");
//			return;
//		}
//
//		Method method = this.handlerMapping.get(url);
//		//method.invoke(arg0, arg1);
//		System.out.println(method);
//
//	}

	private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		
		Handler handler = getHandler(req);
		if(handler == null) {
			resp.getWriter().write("Not Found 404 !!!");
			return;
			
		}
		
		//获取方法参数列表
		Class<?>[] paramTypes = handler.method.getParameterTypes();
		//保存所有需要自动赋值的参数值
		Object[] paramValues = new Object[paramTypes.length];
		
		Map<String,String[]> parms = req.getParameterMap();
		
		try {
			for (Entry<String, String[]> param : parms.entrySet()) {
				String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", "");
				
				if(!handler.paramIndexMapping.containsKey(param.getKey())) continue;
				
				int index = handler.paramIndexMapping.get(param.getKey());
				paramValues[index] = convert(paramTypes[index],value);
			}
			
			//设置方法中的request和response对象
			if(handler.paramIndexMapping.containsKey(HttpServletRequest.class.getName())) {
				int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
				paramValues[reqIndex] = req;
			}
			if(handler.paramIndexMapping.containsKey(HttpServletResponse.class.getName())) {
				int respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
				paramValues[respIndex] = resp;
			}
			System.out.println(handler.method);
			
			handler.method.invoke(handler.controller, paramValues);
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}

	private Handler getHandler(HttpServletRequest req) {
		if(handlerMapping.isEmpty()) return null;
		
		String url = req.getRequestURI();
		String contextPath = req.getContextPath();
		url = url.replace(contextPath, "").replaceAll("/+", "/");
		
		for (Handler handler : handlerMapping) {
			Matcher matcher = handler.pattern.matcher(url);
			
			if(!matcher.matches()) continue;
			return handler;
		}
		
		return null;
	}

	private Object convert(Class<?> type, String value) {
		if(Integer.class == type) {
			return Integer.valueOf(value);
		}
		return value;
	}

	private String firstLowcase(String simpleName) {
		char[] charArray = simpleName.toCharArray();
		charArray[0] += 32;
		return String.valueOf(charArray);
	}

	
	private class Handler {
		
		protected Object controller;
		protected Method method;
		protected Pattern pattern;
		protected Map<String, Integer> paramIndexMapping;
		
		public Handler(Object controller, Method method, Pattern pattern) {
			this.controller = controller;
			this.method = method;
			this.pattern = pattern;
			this.paramIndexMapping = new HashMap<>();
			putParamIndexMapping(method);
		}

		private void putParamIndexMapping(Method method) {
			
			//提取方法中的加了注解的参数
			Annotation[][] annos = method.getParameterAnnotations();
			for (int i = 0; i < annos.length; i++) {
				for (Annotation a : annos[i]) {
					if(a instanceof DemoRequestParam) {
						String paramName = ((DemoRequestParam) a).value();
						if(!"".equals(paramName.trim())) {
							paramIndexMapping.put(paramName, i);
						}
					}
				}
			}
			
			//提取方法中的request和response参数
			Class<?>[] parameterTypes = method.getParameterTypes();
			for (int i = 0; i < parameterTypes.length; i++) {
				Class<?> type = parameterTypes[i];
				if(type == HttpServletRequest.class || type == HttpServletResponse.class) {
					paramIndexMapping.put(type.getName(), i);
				}
			}
		}
		
		
	}
}
