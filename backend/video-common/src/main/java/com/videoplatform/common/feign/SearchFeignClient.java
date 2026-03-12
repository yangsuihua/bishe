package com.videoplatform.common.feign;

import com.videoplatform.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(value = "video-search", path = "/search/inner")
public interface SearchFeignClient {

    @PostMapping("/sync/{videoId}")
    Result<Void> syncVideo(@PathVariable("videoId") Long videoId);

    @DeleteMapping("/sync/{videoId}")
    Result<Void> deleteVideo(@PathVariable("videoId") Long videoId);
}
