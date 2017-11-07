package com.vivareal.search.api.configuration;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class NewRelicTransactionInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object object) throws Exception {
        String servletPath = request.getServletPath();
        if(servletPath != null && !servletPath.startsWith("/v2"))
            request.setAttribute("com.newrelic.agent.IGNORE", true);

        request.setAttribute("startTime", System.nanoTime());

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object object, ModelAndView model) throws Exception {
        if(!HttpStatus.valueOf(response.getStatus()).is5xxServerError() && (System.nanoTime() - (Long)request.getAttribute("startTime")) > 5000000000L)
            request.setAttribute("com.newrelic.agent.IGNORE", true);
    }
}
