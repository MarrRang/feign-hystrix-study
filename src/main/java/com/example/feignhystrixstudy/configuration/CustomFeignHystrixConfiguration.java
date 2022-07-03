package com.example.feignhystrixstudy.configuration;

import com.example.feignhystrixstudy.annotation.CustomFeignHystrix;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.netflix.hystrix.HystrixCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@Configuration
public class CustomFeignHystrixConfiguration {
	@Bean
	public CircuitBreakerFactory CustomHystrixCircuitBreakerFactory() {
		HystrixCircuitBreakerFactory circuitBreakerFactory = new HystrixCircuitBreakerFactory();

		List<Customizer<HystrixCircuitBreakerFactory>> customizerList = getCircuitBreakerCustomizer();

		customizerList.forEach(customizer -> customizer.customize(circuitBreakerFactory));

		//default 설정
		circuitBreakerFactory.configureDefault(id -> HystrixCommand.Setter
				.withGroupKey(HystrixCommandGroupKey.Factory.asKey(id))
				.andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
						.withExecutionTimeoutEnabled(true)
						.withExecutionTimeoutInMilliseconds(3000)
				)
				.andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
						.withCoreSize(30))
		);

		return circuitBreakerFactory;
	}

	private List<Customizer<HystrixCircuitBreakerFactory>> getCircuitBreakerCustomizer() {
		Reflections reflections = new Reflections(new ConfigurationBuilder()
				.setUrls(ClasspathHelper.forPackage("com.example.feignhystrixstudy")).setScanners(Scanners.MethodsAnnotated));

		Set<Method> methodSet = reflections.getMethodsAnnotatedWith(CustomFeignHystrix.class);

		List<Customizer<HystrixCircuitBreakerFactory>> customizerList = new ArrayList<>();

		try {
			methodSet.forEach(method -> {
				String superClassName = method.getDeclaringClass().getSimpleName();
				String methodName = method.getName();
				Class<?>[] methodParameterClasses = method.getParameterTypes();
				List<String> parameterClassNameList = Arrays.stream(methodParameterClasses).map(Class::getSimpleName).toList();

				CustomFeignHystrix customFeignHystrix = method.getAnnotation(CustomFeignHystrix.class);
				HystrixCommandProperties.Setter setter = HystrixCommandProperties.Setter()
						.withExecutionIsolationThreadTimeoutInMilliseconds(customFeignHystrix.executionIsolationThreadTimeoutInMilliseconds())
						.withExecutionTimeoutInMilliseconds(customFeignHystrix.executionTimeoutInMilliseconds())
						.withExecutionTimeoutEnabled(customFeignHystrix.executionTimeoutEnabled())
						.withMetricsRollingStatisticalWindowInMilliseconds(customFeignHystrix.metricsRollingStatisticalWindowInMilliseconds())
						.withCircuitBreakerEnabled(true)
						.withCircuitBreakerRequestVolumeThreshold(customFeignHystrix.circuitBreakerRequestVolumeThreshold())
						.withCircuitBreakerErrorThresholdPercentage(customFeignHystrix.circuitBreakerErrorThresholdPercentage())
						.withCircuitBreakerSleepWindowInMilliseconds(customFeignHystrix.circuitBreakerSleepWindowInMilliseconds());

				Customizer<HystrixCircuitBreakerFactory> factoryCustomizer = getCustomHystrixCustomizer(superClassName, methodName, parameterClassNameList, setter);

				customizerList.add(factoryCustomizer);
			});
		} catch (RuntimeException e) {
			log.error("CustomHystrix Bean Setting Error : ", e);
		}

		return customizerList;
	}

	private Customizer<HystrixCircuitBreakerFactory> getCustomHystrixCustomizer(
			String className,
			String methodName,
			List<String> parameterClassList,
			HystrixCommandProperties.Setter options
	) {
		// Feign에서 찾는 CommandKey가 {클래스명}#{메서드명}({파라미터 클래스명}...) 형태
		String id = className + "#" + methodName + "(" + String.join(",", parameterClassList) + ")";

		return HystrixCircuitBreakerFactory -> HystrixCircuitBreakerFactory.configure(
				hystrixConfigBuilder -> hystrixConfigBuilder.commandProperties(options), id
		);
	}
}
