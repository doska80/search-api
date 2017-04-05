package com.vivareal.search.api.controller.v2;

import com.vivareal.search.api.model.SearchRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/listings")
public class ListingController {

    @RequestMapping("/")
    public String getListings(SearchRequest request) {
        return "foi";
    }


}
