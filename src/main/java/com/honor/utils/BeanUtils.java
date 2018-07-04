package com.honor.utils;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

public class BeanUtils {
	
	/**
	 * 判断是否是基本类型
	 * @return
	 */
	public static boolean isBaseType(Class<?> clazz ) {
		if (clazz.equals(java.lang.Integer.class) ||
		        clazz.equals(java.lang.Byte.class) ||
		        clazz.equals(java.lang.Long.class) ||
		        clazz.equals(java.lang.Double.class) ||
		        clazz.equals(java.lang.Float.class) ||
		        clazz.equals(java.lang.Character.class) ||
		        clazz.equals(java.lang.Short.class) ||
		        clazz.equals(java.lang.Boolean.class)) {
		        return true;
		}
	    return false;
	}
	
	public static boolean isCommonType(Class<?> clazz) {
		if (clazz.equals(java.lang.Integer.class) ||
		        clazz.equals(java.lang.String.class) ||
		        clazz.equals(java.lang.Double.class) ||
		        clazz.equals(java.lang.Boolean.class)) {
		        return true;
		}
	    return false;
	}
	
	/**
	 * javassist获取函数参数名称 
	 * @param targetClass 目标类全限定名
	 * @param targetMethodName 方法名
	 * @return
	 */
	public static String[] getMethodVariableNames(String targetClass, String targetMethodName) {  
	    try {  
	    	Class<?> clazz = Class.forName(targetClass);  
	    	ClassPool pool = ClassPool.getDefault();  
	    	//pool.insertClassPath(new ClassClassPath(clazz));  
	    	CtClass	cc = pool.get(clazz.getName());  
	    	CtMethod cm = cc.getDeclaredMethod(targetMethodName);  
	    	
	    	// 使用javaassist的反射方法获取方法的参数名  
	    	MethodInfo methodInfo = cm.getMethodInfo();  
	    	CodeAttribute codeAttribute = methodInfo.getCodeAttribute();  
	    	LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);  
	    	
	    	String[] variableNames = new String[cm.getParameterTypes().length];  
	    	
	    	int staticIndex = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;  
	    	for (int i = 0; i < variableNames.length; i++)  {
	    		variableNames[i] = attr.variableName(i + staticIndex);  
	    	}
	    	return variableNames;  
	    } catch (ClassNotFoundException e) {  
	        e.printStackTrace();  
	    } catch (NotFoundException e1) {
			e1.printStackTrace();
		}
		return null;  
	}
	
}
