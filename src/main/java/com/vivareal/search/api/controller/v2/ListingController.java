package com.vivareal.search.api.controller.v2;

import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.SearchApiResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/listings")
public class ListingController {

    @RequestMapping("/")
    public SearchApiResponse getListings(SearchApiRequest request) {
        return new SearchApiResponse("foi memo!");
    }
}