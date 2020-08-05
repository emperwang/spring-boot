package com.wk.config;

import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ServletComponentScan(basePackages = {"com.wk.servlet"})
public class ServletConfig {
}
