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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;

/**
 * Abstract base class for handlers of Servlet components discovered via classpath
 * scanning.
 *
 * @author Andy Wilkinson
 */
abstract class ServletComponentHandler {

	private final Class<? extends Annotation> annotationType;

	private final TypeFilter typeFilter;

	protected ServletComponentHandler(Class<? extends Annotation> annotationType) {
		this.typeFilter = new AnnotationTypeFilter(annotationType);
		this.annotationType = annotationType;
	}

	TypeFilter getTypeFilter() {
		return this.typeFilter;
	}

	protected String[] extractUrlPatterns(Map<String, Object> attributes) {
		String[] value = (String[]) attributes.get("value");
		String[] urlPatterns = (String[]) attributes.get("urlPatterns");
		if (urlPatterns.length > 0) {
			Assert.state(value.length == 0, "The urlPatterns and value attributes are mutually exclusive.");
			return urlPatterns;
		}
		return value;
	}

	protected final Map<String, String> extractInitParameters(Map<String, Object> attributes) {
		Map<String, String> initParameters = new HashMap<>();
		for (AnnotationAttributes initParam : (AnnotationAttributes[]) attributes.get("initParams")) {
			String name = (String) initParam.get("name");
			String value = (String) initParam.get("value");
			initParameters.put(name, value);
		}
		return initParameters;
	}
	// 对那些 带有 WebServlet  WebFilter  WebListener的bean的处理
	void handle(AnnotatedBeanDefinition beanDefinition, BeanDefinitionRegistry registry) {
		// 当然了,这里就会根据具体的注解信息,最终匹配到合适的 处理器
		Map<String, Object> attributes = beanDefinition.getMetadata()
				.getAnnotationAttributes(this.annotationType.getName());
		// 运行到此,说明当前的处理器和此bean是符合的
		// 如 WebServletHandler 处理 WebServlet注解bean
		if (attributes != null) {
			// 模板方法,由具体处理器 子类来实现
			doHandle(attributes, beanDefinition, registry);
		}
	}

	protected abstract void doHandle(Map<String, Object> attributes, AnnotatedBeanDefinition beanDefinition,
			BeanDefinitionRegistry registry);

}
