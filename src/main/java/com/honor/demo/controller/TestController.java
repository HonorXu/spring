package com.honor.demo.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.honor.demo.service.IDemoService;
import com.honor.servlet.annotation.DemoAutowired;
import com.honor.servlet.annotation.DemoRequestMapping;
import com.honor.servlet.annotation.DemoRequestParam;

@com.honor.servlet.annotation.DemoController
@DemoRequestMapping("/demo")
public class TestController {
	
	@DemoAutowired
	private IDemoService demoService;
	
	@DemoRequestMapping("/add")
	public void add(@DemoRequestParam("a") Integer a, @DemoRequestParam("b")Integer b, HttpServletResponse resp) throws IOException {
		String s = demoService.add(a,b);
		resp.getWriter().write(s);
	}
	
	@DemoRequestMapping("/getName")
	public void getName(@DemoRequestParam("name")String name, HttpServletResponse resp) throws IOException {
		String s = demoService.getName(name);
		resp.getWriter().write(s);
	}

	@DemoRequestMapping("/getInfo")
	public void getInfo(User user, HttpServletResponse resp) throws IOException {
		String s = demoService.getInfo(user);
		resp.getWriter().write(s);
	}
}
