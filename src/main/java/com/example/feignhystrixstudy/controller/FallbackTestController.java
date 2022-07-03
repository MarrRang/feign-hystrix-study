package com.example.feignhystrixstudy.controller;

import com.example.feignhystrixstudy.client.ProviderApiClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackTestController {
	private final ProviderApiClient providerApiClient;

	public FallbackTestController(ProviderApiClient providerApiClient) {
		this.providerApiClient = providerApiClient;
	}

	@GetMapping
	public String fallbackTestWithSeconds(@RequestParam int sleepTime, @RequestParam boolean withCustomHystrix) {
		if (withCustomHystrix) {
			return providerApiClient.provide(sleepTime);
		} else {
			return providerApiClient.provideWithNoHystrix(sleepTime);
		}
	}

	@GetMapping("/sleep")
	public String sleep(@RequestParam int sleepTime) {
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return "Sleep Success";
	}
}
