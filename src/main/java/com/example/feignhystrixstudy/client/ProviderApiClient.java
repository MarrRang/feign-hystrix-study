package com.example.feignhystrixstudy.client;

import com.example.feignhystrixstudy.annotation.CustomFeignHystrix;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Repository
@FeignClient(
		name = "provider-api-client",
		url = "http://localhost:8080",
		fallback = ProviderApiClientFallback.class
)
public interface ProviderApiClient {
	@CustomFeignHystrix(
			executionTimeoutInMilliseconds = 1000
	)
	@GetMapping(
			value = "/sleep",
			produces = "application/json",
			headers = {"User-Agent=FeignClient", "Cache-Control=no-cache"}
	)
	String provide(@RequestParam int sleepTime);

	@GetMapping(
			value = "/sleep",
			produces = "application/json",
			headers = {"User-Agent=FeignClient", "Cache-Control=no-cache"}
	)
	String provideWithNoHystrix(@RequestParam int sleepTime);
}
