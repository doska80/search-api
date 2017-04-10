package com.vivareal.search.api.controller.v2;

import com.vivareal.search.api.model.SearchRequest;
import com.vivareal.search.api.model.SearchResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/v2/listings")
public class ListingController {

    @RequestMapping("/")
    public SearchResponse getListings(SearchRequest request) {
        return new SearchResponse("foi");
    }
}