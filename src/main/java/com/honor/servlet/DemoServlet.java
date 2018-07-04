package com.honor.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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
import com.honor.utils.BeanUtils;

public class DemoServlet extends HttpServlet {

	private static final long serialVersionUID = 7562061117755667137L;
	
	private Properties contextConfig = new Properties();
	
	private List<String> classNames = new ArrayList<>();
	
	private Map<String,Object> ioc = new HashMap<>();
	
	private List<Handler> handlerMapping = new ArrayList<>();
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doDispatcher(req, resp);
	}


	@Override
	public void init(ServletConfig config) throws ServletException {
		//1.加载配置文件application.properties
		String contextConfigLocation = config.getInitParameter("contextConfigLocation");
		loadConfig(contextConfigLocation);
		//2.扫描scanPackage
		String scanPackage = this.contextConfig.getProperty("scanPackage");
		scanPackage(scanPackage);
		//3.实例化ioc
		doInstance();
		System.out.println(classNames);
		System.out.println(ioc);
		//4.依赖注入DI
		doAutowired();
		//5.初始化handlerMapping
		initHandlerMapping();
		
	}
	
	
	private void loadConfig(String contextConfigLocation) {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
		try {
			contextConfig.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void scanPackage(String scanPackage) {
		String path = "/" + scanPackage.replace(".", "/");
		URL url = this.getClass().getResource(path);
		File classDir = new File(url.getFile());
		for (File file : classDir.listFiles()) {
			if(file.isDirectory()) {
				scanPackage(scanPackage + "." + file.getName());
			}else {
				String className = scanPackage + "." + file.getName().replace(".class", "");
				classNames.add(className);
			}
		}
	}

	private void doInstance() {
		
		if(classNames.isEmpty()) return;
		
		try {
			for (String className : classNames) {
				Class<?> clazz = Class.forName(className);
				if(clazz.isAnnotationPresent(DemoController.class)) {
					ioc.put(lowwerFirstCase(clazz.getSimpleName()), clazz.newInstance());
				}else if(clazz.isAnnotationPresent(DemoService.class)) {
					String beanName = clazz.getAnnotation(DemoService.class).value();
					if("".equals(beanName)) {
						beanName = lowwerFirstCase(clazz.getSimpleName());
					}
					Object instance = clazz.newInstance();
					ioc.put(beanName, instance);
					
					Class<?>[] interfaces = clazz.getInterfaces();
					for (Class<?> i : interfaces) {
						ioc.put(i.getName(), instance);
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private void doAutowired() {
		if(ioc.isEmpty()) return;
		try {
			for (Entry<String, Object> entry : ioc.entrySet()) {
				Class<? extends Object> clazz = entry.getValue().getClass();
				if(clazz.isAnnotationPresent(DemoController.class)) {
					Field[] fields = clazz.getDeclaredFields();
					for (Field field : fields) {
						if(field.isAnnotationPresent(DemoAutowired.class)) {
							String beanName = field.getType().getName();
							field.setAccessible(true);
							field.set(entry.getValue(), ioc.get(beanName));
						}
					}
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private void initHandlerMapping() {
		if(ioc.isEmpty()) return;
		
		for (Entry<String, Object> entry : ioc.entrySet()) {
			Class<? extends Object> clazz = entry.getValue().getClass();
			if(clazz.isAnnotationPresent(DemoController.class)) {
				String basePath ="/" + clazz.getAnnotation(DemoRequestMapping.class).value();
				
				Method[] methods = clazz.getMethods();
				for (Method method : methods) {
					if(method.isAnnotationPresent(DemoRequestMapping.class)) {
						String path = method.getAnnotation(DemoRequestMapping.class).value();
						path = (basePath + "/" + path).replaceAll("/+", "/");
						Pattern pattern = Pattern.compile(path);
						handlerMapping.add(new Handler(method,entry.getValue(),pattern));
					}
				}
			}
		}
	}

	private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Handler handler = getHandler(req);
		
		if(handler ==null) {
			resp.getWriter().write("Not Found 404 !!!");
			return;
		}
		
		Class<?>[] types = handler.method.getParameterTypes();
		Object[] params = new Object[types.length];
		
		Map<String, String[]> paramMap = req.getParameterMap();
		
		for (Entry<String, String[]> entry : paramMap.entrySet()) {
			String value = Arrays.toString(entry.getValue()).replaceAll("\\[|\\]", "");
			if(handler.indexOfParam.containsKey(entry.getKey())) {
				int index = handler.indexOfParam.get(entry.getKey());
				params[index] = convert(types[index], value);
			}
		}
		
		if(handler.indexOfParam.containsKey(HttpServletRequest.class.getName())) {
			int index = handler.indexOfParam.get(HttpServletRequest.class.getName());
			params[index] = req;
		}

		if(handler.indexOfParam.containsKey(HttpServletResponse.class.getName())) {
			int index = handler.indexOfParam.get(HttpServletResponse.class.getName());
			params[index] = resp;
		}
		
		try {
			handler.method.invoke(handler.controller, params);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	
	
	private Handler getHandler(HttpServletRequest req) {
		if(this.handlerMapping.isEmpty()) return null;
		
		String uri = req.getRequestURI();
		String contextPath = req.getContextPath();
		uri = uri.replace(contextPath, "").replaceAll("/+", "/");
		
		for (Handler handler : handlerMapping) {
			Matcher matcher = handler.pattern.matcher(uri);
			if(matcher.matches()) return handler;
		}
		return null;
	}

	private String lowwerFirstCase(String value) {
		char[] cs = value.toCharArray();
		cs[0] += 32;
		return value;
	}
	
	private Object convert(Class<?> type, String value) {
		if(Integer.class == type) {
			return Integer.valueOf(value);
		}
		return value;
	}
	
	@SuppressWarnings("unused")
	public class Handler {
		
		private Method method;
		
		private Object controller;
		
		private Pattern pattern;
		
		private Map<String,Integer> indexOfParam;

		public Handler(Method method, Object controller, Pattern pattern) {
			super();
			this.method = method;
			this.controller = controller;
			this.pattern = pattern;
			this.indexOfParam = getIndexOfParam(method);
//			this.indexOfParam = getIndexOfParam(controller,method);
			System.out.println(method.getName() + ":\t" + indexOfParam);
		}
		
		/**
		 * before java 1.8
		 * @param controller
		 * @param method
		 * @return
		 */
		private Map<String, Integer> getIndexOfParam(Object controller, Method method) {
			Map<String,Integer> indexOfParam = new HashMap<>();
			//javassist获取函数参数名称 
			String[] names = BeanUtils.getMethodVariableNames(controller.getClass().getName(), method.getName());
			for (int i = 0; i < names.length; i++) {
				indexOfParam.put(names[i], i);
			}
			return indexOfParam;
		}
		
		/**
		 * Method.getParameters为1.8新增方法，可以获取参数信息，包括参数名称
		 * 保留参数名这一选项由编译开关javac -parameters打开，默认是关闭的
		 * @param method
		 * @return
		 */
		private Map<String, Integer> getIndexOfParam(Method method) {
			Map<String,Integer> indexOfParam = new HashMap<>();
			
			Parameter[] parameters = method.getParameters();
			for (int i = 0; i < parameters.length; i++) {
				if(BeanUtils.isCommonType(parameters[i].getType())) {
					String param = parameters[i].getName();
					if(parameters[i].isAnnotationPresent(DemoRequestParam.class)) {
						param = parameters[i].getAnnotation(DemoRequestParam.class).value();
					}
					indexOfParam.put(param, i);
				}else if(parameters[i].getType() == HttpServletRequest.class 
						|| parameters[i].getType() == HttpServletResponse.class) {
					indexOfParam.put(parameters[i].getType().getName(), i);
				}else {
					//TODO 自定义类型
				}
			}
			return indexOfParam;
		}
		
	}

}
