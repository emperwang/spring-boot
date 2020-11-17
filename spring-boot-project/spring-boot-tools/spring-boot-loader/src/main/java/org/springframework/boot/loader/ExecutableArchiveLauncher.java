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

package org.springframework.boot.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

import org.springframework.boot.loader.archive.Archive;

/**
 * Base class for executable archive {@link Launcher}s.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public abstract class ExecutableArchiveLauncher extends Launcher {

	private final Archive archive;

	public ExecutableArchiveLauncher() {
		try {
			// 这里相当于时 获取到了代码的路径
			// 如 C:/code-workspace/source/JavaBase/JavaBaseProject/target/classes/
			this.archive = createArchive();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected ExecutableArchiveLauncher(Archive archive) {
		this.archive = archive;
	}

	protected final Archive getArchive() {
		return this.archive;
	}
	// 获取具体要执行的全类名
	@Override
	protected String getMainClass() throws Exception {
		Manifest manifest = this.archive.getManifest();
		String mainClass = null;
		if (manifest != null) {
			// 要启动的类是  Start-Class 对应的属性值
			mainClass = manifest.getMainAttributes().getValue("Start-Class");
		}
		if (mainClass == null) {
			throw new IllegalStateException("No 'Start-Class' manifest entry specified in " + this);
		}
		return mainClass;
	}
	// 这里是获取代理路径上的 Archive,即 jar包
	@Override
	protected List<Archive> getClassPathArchives() throws Exception {
		List<Archive> archives = new ArrayList<>(this.archive.getNestedArchives(this::isNestedArchive));
		postProcessClassPathArchives(archives);
		return archives;
	}

	/**
	 * Determine if the specified {@link JarEntry} is a nested item that should be added
	 * to the classpath. The method is called once for each entry.
	 * @param entry the jar entry
	 * @return {@code true} if the entry is a nested item (jar or folder)
	 */
	protected abstract boolean isNestedArchive(Archive.Entry entry);

	/**
	 * Called to post-process archive entries before they are used. Implementations can
	 * add and remove entries.
	 * @param archives the archives
	 * @throws Exception if the post processing fails
	 */
	protected void postProcessClassPathArchives(List<Archive> archives) throws Exception {
	}

}
