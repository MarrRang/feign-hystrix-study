package com.example.feignhystrixstudy.client;

import org.springframework.stereotype.Component;

@Component
public class ProviderApiClientFallback implements ProviderApiClient {
	@Override
	public String provide(int sleepTime) {
		System.out.println("Fallback!! - provide");

		return "Fallback!! - provide";
	}

	@Override
	public String provideWithNoHystrix(int sleepTime) {
		System.out.println("Fallback!! - provideWithNoHystrix");

		return "Fallback!! - provideInMilliseconds";
	}
}
