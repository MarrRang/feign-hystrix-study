package com.example.feignhystrixstudy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
//AutoConfigure를 사용하지 않을 예정이기에 주석처리
//@EnableHystrix
public class FeignHystrixStudyApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeignHystrixStudyApplication.class, args);
	}

}
