package com.grupozap.search.api.infrastructure;

import static com.grupozap.search.api.infrastructure.RequestLoggingContextFilter.REQUEST_QUERY_STRING;
import static com.grupozap.search.api.infrastructure.RequestLoggingContextFilter.REQUEST_URI;
import static com.grupozap.search.api.infrastructure.RequestLoggingContextFilter.REQUEST_USER_AGENT;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.MDC;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MDC.class})
public class RequestLoggingContextFilterTest {

  private RequestLoggingContextFilter subject = new RequestLoggingContextFilter();

  @Before
  public void setUp() {
    mockStatic(MDC.class);
  }

  @Test
  public void shouldSetMDCWithTheRequestContext() throws ServletException, IOException {
    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    var chain = mock(FilterChain.class);

    when(request.getHeader(RequestLoggingContextFilter.REQUEST_HEADER_USER_AGENT))
        .thenReturn("random-user-agent");
    when(request.getRequestURI()).thenReturn("/v2/listings");
    when(request.getQueryString())
        .thenReturn("includeFields=id&filter=parentId:null AND listingType:\"USED\"");

    subject.doFilterInternal(request, response, chain);

    verifyStatic(MDC.class);
    MDC.put(REQUEST_QUERY_STRING, "includeFields=id&filter=parentId:null AND listingType:\"USED\"");

    verifyStatic(MDC.class);
    MDC.put(REQUEST_URI, "/v2/listings");

    verifyStatic(MDC.class);
    MDC.put(REQUEST_USER_AGENT, "random-user-agent");

    verifyStatic(MDC.class);
    MDC.remove(REQUEST_URI);

    verifyStatic(MDC.class);
    MDC.remove(REQUEST_QUERY_STRING);

    MDC.remove(REQUEST_USER_AGENT);
  }

  @Test
  public void shouldSetMDCWithTheRequestContextWithoutSomeData()
      throws ServletException, IOException {
    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    var chain = mock(FilterChain.class);

    when(request.getRequestURI()).thenReturn("/v2/listings");
    when(request.getQueryString())
        .thenReturn("includeFields=id&filter=parentId:null AND listingType:\"USED\"");

    subject.doFilterInternal(request, response, chain);

    verifyStatic(MDC.class, times(2));
    MDC.put(anyString(), anyString());

    verifyStatic(MDC.class, times(3));
    MDC.remove(anyString());
  }
}
