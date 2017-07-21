package com.vivareal.search.api.model.http;

import com.vivareal.search.api.model.search.Fetchable;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Set;
@ApiModel
public class BaseApiRequest implements Fetchable {
    @ApiModelProperty(value ="Index name", example = "my_index", required = true, reference = "path")
    private String index;

    @ApiModelProperty(value = "Fields that will be included in the result", example = "field1, field2")
    private Set<String> includeFields;

    @ApiModelProperty(value = "Fields that will be excluded in the result", example = "field1, field2")
    private Set<String> excludeFields;

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public Set<String> getIncludeFields() {
        return includeFields;
    }

    public void setIncludeFields(Set<String> includeFields) {
        this.includeFields = includeFields;
    }

    public Set<String> getExcludeFields() {
        return excludeFields;
    }

    public void setExcludeFields(Set<String> excludeFields) {
        this.excludeFields = excludeFields;
    }
}

