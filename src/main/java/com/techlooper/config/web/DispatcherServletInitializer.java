/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.techlooper.config.web;

import com.techlooper.config.CoreConfiguration;
import com.techlooper.config.VnwDbConfiguration;
import com.techlooper.config.web.sec.SecurityConfiguration;
import com.techlooper.config.web.sec.SessionListener;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.SessionCookieConfig;

public class DispatcherServletInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

  public void onStartup(ServletContext servletContext) throws ServletException {
    super.onStartup(servletContext);
    SessionCookieConfig sessionCookieConfig = servletContext.getSessionCookieConfig();
    sessionCookieConfig.setMaxAge(SessionListener.MAX_INACTIVE_INTERVAL);
    sessionCookieConfig.setHttpOnly(true);
    servletContext.addListener(new SessionListener());
  }

  protected Class<?>[] getRootConfigClasses() {
    return null;
  }

  protected Class<?>[] getServletConfigClasses() {
    return new Class<?>[]{
      CoreConfiguration.class,
      VnwDbConfiguration.class,
      WebConfiguration.class,
      SecurityConfiguration.class
    };
  }

  protected String[] getServletMappings() {
    return new String[]{"/"};
  }

  protected void customizeRegistration(Dynamic registration) {
    registration.setInitParameter("dispatchOptionsRequest", "true");
  }
}
