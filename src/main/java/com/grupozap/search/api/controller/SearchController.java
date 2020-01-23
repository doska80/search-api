package com.grupozap.search.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.ok;

import com.grupozap.search.api.model.http.BaseApiRequest;
import com.grupozap.search.api.model.http.FilterableApiRequest;
import com.grupozap.search.api.model.http.SearchApiRequest;
import com.grupozap.search.api.model.serializer.SearchResponseEnvelope;
import com.grupozap.search.api.service.SearchService;
import com.grupozap.search.api.service.parser.IndexSettings;
import datadog.trace.api.Trace;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/v2")
@Api("v2")
public class SearchController {

  private static final BodyBuilder BUILDER_OK = ok();
  private static final ResponseEntity<Object> NOT_FOUND_RESPONSE = notFound().build();

  @Autowired private SearchService searchService;
  @Autowired private IndexSettings indexSettings;

  @GetMapping(value = "/{index}/{id}", produces = APPLICATION_JSON_VALUE)
  @ApiOperation(value = "Search by index with id", notes = "Returns index by identifier")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Successfully get by id"),
        @ApiResponse(code = 400, message = "Bad parameters request"),
        @ApiResponse(code = 404, message = "Id not found on cluster"),
        @ApiResponse(code = 500, message = "Internal Server Error")
      })
  @Trace
  public ResponseEntity<Object> id(BaseApiRequest request, @PathVariable String id) {
    indexSettings.validateIndex(request);
    final var response = searchService.getById(request, id);
    if (!response.isExists()) return NOT_FOUND_RESPONSE;
    return BUILDER_OK.body(new String(response.getSourceAsBytes()));
  }

  @GetMapping(value = "/{index}", produces = APPLICATION_JSON_VALUE)
  @ApiOperation(value = "Search documents", notes = "Returns query-based documents")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Successfully get documents"),
        @ApiResponse(code = 400, message = "Bad parameters request"),
        @ApiResponse(code = 500, message = "Internal Server Error")
      })
  @Trace
  public ResponseEntity<Object> search(SearchApiRequest request) {
    indexSettings.validateIndex(request);
    final var alias = indexSettings.getIndexByAlias();
    final var response = searchService.search(request);
    return BUILDER_OK.body(new SearchResponseEnvelope<>(alias, response));
  }

  @GetMapping("/{index}/stream")
  @ApiIgnore
  @Trace
  public StreamingResponseBody stream(
      FilterableApiRequest request, HttpServletResponse httpServletResponse) {
    indexSettings.validateIndex(request);
    httpServletResponse.setContentType("application/x-ndjson;charset=UTF-8");
    return out -> searchService.stream(request, out);
  }
}
