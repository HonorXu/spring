package com.honor.demo.service.impl;

import com.honor.demo.controller.User;
import com.honor.demo.service.IDemoService;
import com.honor.servlet.annotation.DemoService;
@DemoService("sdsd")
public class DemoServiceImpl implements IDemoService {

	
	
	public String add(Integer a, Integer b) {
		return a + " + " + b + " = " + (a + b);
	}

	public String getName(String name) {
		return "My name is " + name;
	}

	@Override
	public String getInfo(User user) {
		String name = "My name is " + user.getName();
		String sex = "sex is " + user.getSex();
		String phone = "phone is" + user.getPhone();
		return name + ", " + sex + ", " + phone;
	}

}
