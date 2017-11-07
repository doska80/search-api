package com.vivareal.search.api.configuration;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static java.lang.System.nanoTime;

@Component
public class NewRelicTransactionFilter extends GenericFilterBean {

    private static final long FILTER_THRESHOLD = 5000000000L; // 5 seconds

    private boolean shouldIgnoreTransaction(HttpServletResponse response) {
        try {
            return !HttpStatus.valueOf(response.getStatus()).is5xxServerError();
        } catch(Exception e) {
            return false;
        }
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        final long startTime = nanoTime();

        chain.doFilter(request, response);

        final String servletPath = ((HttpServletRequest) request).getServletPath();
        if((servletPath != null && !servletPath.startsWith("/v2")) || (shouldIgnoreTransaction((HttpServletResponse)response) && (nanoTime() - startTime) > FILTER_THRESHOLD))
            request.setAttribute("com.newrelic.agent.IGNORE", true);
    }
}
