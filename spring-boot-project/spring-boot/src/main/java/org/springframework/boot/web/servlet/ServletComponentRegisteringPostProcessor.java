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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link BeanFactoryPostProcessor} that registers beans for Servlet components found via
 * package scanning.
 *
 * @author Andy Wilkinson
 * @see ServletComponentScan
 * @see ServletComponentScanRegistrar
 */
class ServletComponentRegisteringPostProcessor implements BeanFactoryPostProcessor, ApplicationContextAware {
	// 记录一些处理器
	private static final List<ServletComponentHandler> HANDLERS;

	static {
		// 在静态代码块中添加一些 handler
		List<ServletComponentHandler> servletComponentHandlers = new ArrayList<>();
		servletComponentHandlers.add(new WebServletHandler());
		servletComponentHandlers.add(new WebFilterHandler());
		servletComponentHandlers.add(new WebListenerHandler());
		// 并记录到  HANDLERS
		HANDLERS = Collections.unmodifiableList(servletComponentHandlers);
	}
	// 记录要扫描的路径
	private final Set<String> packagesToScan;
	// 记录容器
	private ApplicationContext applicationContext;
	// 通过构造器 来注入 要扫描的路径
	ServletComponentRegisteringPostProcessor(Set<String> packagesToScan) {
		this.packagesToScan = packagesToScan;
	}
	// 此操作就是把具体的servlet注册到容器中,看一下其是如何进行处理的
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// 此操作是,必须是web的情况下才进行; 很缜密
		if (isRunningInEmbeddedWebServer()) {
			// 创建一个 候选组件提供者CandidateComponentProvider
			ClassPathScanningCandidateComponentProvider componentProvider = createComponentProvider();
			// 对要扫描的各个路径,进行扫描动作,并把其注册到容器中
			for (String packageToScan : this.packagesToScan) {
				scanPackage(componentProvider, packageToScan);
			}
		}
	}
	// 对路径进行扫描动作
	private void scanPackage(ClassPathScanningCandidateComponentProvider componentProvider, String packageToScan) {
		// 查找路径中潜在的  component
		// findCandidateComponents此处查找候选者,主要是根据上面添加的那些 TypeFilter来进行过滤的
		// 也就是扫描那些 带有 WebServlet  WebFilter WebListener注解的bean
		for (BeanDefinition candidate : componentProvider.findCandidateComponents(packageToScan)) {
			if (candidate instanceof AnnotatedBeanDefinition) {
				// 然后所有的候选者，进行处理
				for (ServletComponentHandler handler : HANDLERS) {
					// 处理候选者
					handler.handle(((AnnotatedBeanDefinition) candidate),
							(BeanDefinitionRegistry) this.applicationContext);
				}
			}
		}
	}

	private boolean isRunningInEmbeddedWebServer() {
		return this.applicationContext instanceof WebApplicationContext
				&& ((WebApplicationContext) this.applicationContext).getServletContext() == null;
	}
	// 创建ClassPathScanningCandidateComponentProvider, 并把 初始化的处理器 记录到其中
	private ClassPathScanningCandidateComponentProvider createComponentProvider() {
		// 创建动作
		ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(
				false);
		// 记录 environment
		componentProvider.setEnvironment(this.applicationContext.getEnvironment());
		// 记录 resourceLoader
		componentProvider.setResourceLoader(this.applicationContext);
		// 添加 过滤类型
		for (ServletComponentHandler handler : HANDLERS) {
			// 其实这里的类型,在handler创建时就指定了
			// 如WebServletHandler 的typeFilter为 WebServlet 注解
			// 这里添加这些 filter, 那么在classpath中查找  潜在的候选者时,就会使用这些filter进行过滤
			// 符合的,才是候选组件
			componentProvider.addIncludeFilter(handler.getTypeFilter());
		}
		return componentProvider;
	}

	Set<String> getPackagesToScan() {
		return Collections.unmodifiableSet(this.packagesToScan);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
