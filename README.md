# feign-hystrix-study
<details open>
<summary>In Korean</summary>

## 개요
Feign Client에서 Hystrix를 이용한 Fallback 설정을 메서드 별로 설정하기 위해
Hystrix Configuration을 Custom Annotation형태로 등록하도록 함

## 작동 방식
@CustomFeignHystrix annotation이 붙은 메서드를 찾아 사용자가 설정한 값으로 Hystrix factory에 등록

## 개발 환경
* Spring Boot 2.7.1
* Java 17
* Maven

## 프로세스

### 1. Dependency 등록
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
    <version>2.2.10.RELEASE</version>
</dependency>
<dependency>
    <groupId>org.reflections</groupId>
    <artifactId>reflections</artifactId>
    <version>0.10.2</version>
</dependency>
```

### 2. Feign CircuitBreaker 활성화
```
# application.yml
feign:
  circuitbreaker:
    enabled: true
```

### 3. Custom Annotation 작성
#### In Example Code : [CustomFeignHystrix](https://github.com/MarrRang/feign-hystrix-study/blob/master/src/main/java/com/example/feignhystrixstudy/annotation/CustomFeignHystrix.java)
```
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface CustomFeignHystrix {
    String value() default "";

    int executionIsolationThreadTimeoutInMilliseconds() default 3000;

    int executionTimeoutInMilliseconds() default 3000;

    boolean executionTimeoutEnabled() default true;

    int metricsRollingStatisticalWindowInMilliseconds() default 10000;

    int circuitBreakerRequestVolumeThreshold() default 10;

    int circuitBreakerErrorThresholdPercentage() default 50;

    int circuitBreakerSleepWindowInMilliseconds() default 5000;
}
```

### 4. Configuration 작성
#### In Example Code : [CustomFeignHystrixConfiguration](https://github.com/MarrRang/feign-hystrix-study/blob/master/src/main/java/com/example/feignhystrixstudy/configuration/CustomFeignHystrixConfiguration.java)
```
@Slf4j
@Configuration
public class CustomFeignHystrixConfiguration {
	@Bean
	public CircuitBreakerFactory CustomHystrixCircuitBreakerFactory() {
		HystrixCircuitBreakerFactory circuitBreakerFactory = new HystrixCircuitBreakerFactory();

		List<Customizer<HystrixCircuitBreakerFactory>> customizerList = getCircuitBreakerCustomizer();

		customizerList.forEach(customizer -> customizer.customize(circuitBreakerFactory));

		//default
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
```

5. Feign Client에 사용
#### In Example Code : [ProviderApiClient](https://github.com/MarrRang/feign-hystrix-study/blob/master/src/main/java/com/example/feignhystrixstudy/client/ProviderApiClient.java)
```
@FeignClient(...)
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
}
```
</details>

<details>
<summary>In English</summary>

# feign-hystrix-study

## Summary
Allow the Feign Client to register the Hystrix Configuration in the form of Custom Annotation in order to set Fallback settings using Hystrix by method

## How it works
Find a method with *@CustomFeignHystrix* annotation and register it with the value you set in the Hystrix factory

## Environment
* Spring Boot 2.7.1
* Java 17
* Maven

## Process

### 1. Register dependency
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
    <version>2.2.10.RELEASE</version>
</dependency>
<dependency>
    <groupId>org.reflections</groupId>
    <artifactId>reflections</artifactId>
    <version>0.10.2</version>
</dependency>
```

### 2. Enable Feign CircuitBreaker
```
# application.yml
feign:
  circuitbreaker:
    enabled: true
```

### 3. Add custom annotation
#### In Example Code : [CustomFeignHystrix](https://github.com/MarrRang/feign-hystrix-study/blob/master/src/main/java/com/example/feignhystrixstudy/annotation/CustomFeignHystrix.java)
```
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface CustomFeignHystrix {
    String value() default "";

    int executionIsolationThreadTimeoutInMilliseconds() default 3000;

    int executionTimeoutInMilliseconds() default 3000;

    boolean executionTimeoutEnabled() default true;

    int metricsRollingStatisticalWindowInMilliseconds() default 10000;

    int circuitBreakerRequestVolumeThreshold() default 10;

    int circuitBreakerErrorThresholdPercentage() default 50;

    int circuitBreakerSleepWindowInMilliseconds() default 5000;
}
```

### 4. Add Configuration
#### In Example Code : [CustomFeignHystrixConfiguration](https://github.com/MarrRang/feign-hystrix-study/blob/master/src/main/java/com/example/feignhystrixstudy/configuration/CustomFeignHystrixConfiguration.java)
```
@Slf4j
@Configuration
public class CustomFeignHystrixConfiguration {
	@Bean
	public CircuitBreakerFactory CustomHystrixCircuitBreakerFactory() {
		HystrixCircuitBreakerFactory circuitBreakerFactory = new HystrixCircuitBreakerFactory();

		List<Customizer<HystrixCircuitBreakerFactory>> customizerList = getCircuitBreakerCustomizer();

		customizerList.forEach(customizer -> customizer.customize(circuitBreakerFactory));

		//default
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
		// Feign find commandKey looks like *{class name}#{methodname} ({parameter classname}...)*
		String id = className + "#" + methodName + "(" + String.join(",", parameterClassList) + ")";

		return HystrixCircuitBreakerFactory -> HystrixCircuitBreakerFactory.configure(
				hystrixConfigBuilder -> hystrixConfigBuilder.commandProperties(options), id
		);
	}
}
```

5. Use custom annotation
#### In Example Code : [ProviderApiClient](https://github.com/MarrRang/feign-hystrix-study/blob/master/src/main/java/com/example/feignhystrixstudy/client/ProviderApiClient.java)
```
@FeignClient(...)
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
}
```
</details>
