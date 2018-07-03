package com.honor.demo.service.impl;

import com.honor.demo.service.IDemoService;
import com.honor.servlet.annotation.DemoService;
@DemoService
public class DemoServiceImpl implements IDemoService {

	
	
	public String add(Integer a, Integer b) {
		return a + " + " + b + " = " + (a + b);
	}

	public String getName(String name) {
		return "My name is " + name;
	}

}
