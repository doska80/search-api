package com.vivareal.search.api.configuration;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class NewRelicTransactionFilter extends GenericFilterBean {

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        final String servletPath = ((HttpServletRequest) request).getServletPath();
        if(servletPath != null && !servletPath.startsWith("/v2"))
            request.setAttribute("com.newrelic.agent.IGNORE", true);

        chain.doFilter(request, response);
    }
}
