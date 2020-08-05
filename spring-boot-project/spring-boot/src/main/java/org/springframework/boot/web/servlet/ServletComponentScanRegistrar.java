/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.servlet;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} used by {@link ServletComponentScan}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
// 此注入一个后置处理器到容器中
// 此后置处理器主要就是对容器中的  servlet的bean的处理
class ServletComponentScanRegistrar implements ImportBeanDefinitionRegistrar {

	private static final String BEAN_NAME = "servletComponentRegisteringPostProcessor";
	// 注入一个bean到容器中
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		// 先获取注解中要扫描的路径
		Set<String> packagesToScan = getPackagesToScan(importingClassMetadata);
		// 先查看容器中是否存在了 servletComponentRegisteringPostProcessor  bean
		if (registry.containsBeanDefinition(BEAN_NAME)) {
			// 存在了,则进行更新
			updatePostProcessor(registry, packagesToScan);
		}
		else {
			// 没有存在则添加此bean 到容器中
			addPostProcessor(registry, packagesToScan);
		}
	}
	// 更新servletComponentRegisteringPostProcessor beanDefinition的信息
	private void updatePostProcessor(BeanDefinitionRegistry registry, Set<String> packagesToScan) {
		// 获取servletComponentRegisteringPostProcessor的beanDefinition
		BeanDefinition definition = registry.getBeanDefinition(BEAN_NAME);
		// 构造一个构造器参数
		ValueHolder constructorArguments = definition.getConstructorArgumentValues().getGenericArgumentValue(Set.class);
		@SuppressWarnings("unchecked")
		Set<String> mergedPackages = (Set<String>) constructorArguments.getValue();
		mergedPackages.addAll(packagesToScan);
		// 记录构造器参数
		constructorArguments.setValue(mergedPackages);
	}
	// 添加servletComponentRegisteringPostProcessor  的beanDefinition到容器中
	private void addPostProcessor(BeanDefinitionRegistry registry, Set<String> packagesToScan) {
		// 创建一个beanDefinition
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		// 设置此 bean的beanClass
		beanDefinition.setBeanClass(ServletComponentRegisteringPostProcessor.class);
		// 设置 构造器参数
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(packagesToScan);
		// 记录bean的角色
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 注册beanDefinition
		registry.registerBeanDefinition(BEAN_NAME, beanDefinition);
	}
	// 从注解的属性信息中 获取要扫描的路径
	private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
		// 先获取此注解的 属性信息
		AnnotationAttributes attributes = AnnotationAttributes
				.fromMap(metadata.getAnnotationAttributes(ServletComponentScan.class.getName()));
		// 获取要扫描的路径
		String[] basePackages = attributes.getStringArray("basePackages");
		// 获取保存扫描路径的class
		Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
		// 保存要扫描的路径
		Set<String> packagesToScan = new LinkedHashSet<>();
		// 先把设置的路径记录
		packagesToScan.addAll(Arrays.asList(basePackages));
		// 获取设置的类的 package 路径, 以此作为要扫描的 路径
		for (Class<?> basePackageClass : basePackageClasses) {
			packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
		}
		// 如果没有设置要扫描的路径信息,则使用 此注解所在类的package 路径作为扫描
		// 此是一个特殊的点哦
		if (packagesToScan.isEmpty()) {
			packagesToScan.add(ClassUtils.getPackageName(metadata.getClassName()));
		}
		return packagesToScan;
	}

}
