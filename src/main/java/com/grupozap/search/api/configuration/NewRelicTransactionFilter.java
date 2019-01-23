package com.grupozap.search.api.configuration;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

@Component
public class NewRelicTransactionFilter extends GenericFilterBean {

  @Override
  public void doFilter(
      final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {
    final var servletPath = ((HttpServletRequest) request).getServletPath();
    if (servletPath != null && !servletPath.startsWith("/v2"))
      request.setAttribute("com.newrelic.agent.IGNORE", true);

    chain.doFilter(request, response);
  }
}
