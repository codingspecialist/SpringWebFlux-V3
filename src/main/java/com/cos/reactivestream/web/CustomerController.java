package com.cos.reactivestream.web;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cos.reactivestream.domain.Customer;
import com.cos.reactivestream.domain.CustomerRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;
import reactor.core.publisher.Sinks.EmitResult;

// 참고 : https://spring.io/guides/gs/accessing-data-r2dbc/
// R2DBC 샘플 테스트해서 연결하면 끝날듯..!!

// EmitterProcesser FluxProcesser Sinks.Many 차이
// 참고 : 아직 미정

// 참고 : https://github.com/reactor/reactor-core/blob/master/docs/asciidoc/processors.adoc
// many().multicast(): 새로 푸시 된 데이터 만 구독자에게 전송하여 배압을 준수하는 싱크 ( "구독자의 구독 후"에서처럼 새로 푸시 됨).
// many().unicast(): 위와 동일하며 첫 번째 구독자 레지스터가 버퍼링되기 전에 푸시 된 데이터가 왜곡됩니다.
// many().replay(): 푸시 된 데이터의 지정된 기록 크기를 새 구독자에게 재생 한 다음 새 데이터를 계속해서 실시간으로 푸시하는 싱크입니다.
// one(): 구독자에게 단일 요소를 재생하는 싱크
// empty(): 가입자에게만 터미널 신호를 재생하지만 (오류 또는 완료) 여전히 Mono<T>(일반 유형에주의) 로 볼 수있는 싱크 <T>.

@RestController
public class CustomerController {
	
	private final CustomerRepository customerRepository;
	private final Sinks.Many<String> sink;
	
	public CustomerController(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
		this.sink = Sinks.many().multicast().onBackpressureBuffer();  // unicast, multicast, replay
	}
	
	@GetMapping("/customer")
	public Flux<Customer> findAll(){
		 return customerRepository.findAll();
	}
	
	@GetMapping("/customer/{id}")
	public Mono<Customer> findById(@PathVariable Long id){
		 return customerRepository.findById(id);
	}
	
	@GetMapping(value="/customer/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> findAllSSE(){
		return sink.asFlux().map(e -> ServerSentEvent.builder(e).build()).doOnCancel(()-> {
			System.out.println("sse 종료됨");
			sink.asFlux().blockLast(); // 연결 종료 후에 재연결 가능하게 해줌.
			
		});
	}
	
	// ~~~~~~~~~~~~~~~~~~~  SSE 프로토콜
	// 참고 : https://lts0606.tistory.com/306
	// 참고 : https://stackoverflow.com/questions/51370463/spring-webflux-flux-how-to-publish-dynamically
    @PostMapping("/customer")
    public Mono<Customer> save(@RequestBody Customer customer) {
    	System.out.println("save() 요청됨");
    	
    	// return을 안해주면 트랜잭션 발동이 안됨. 그래서 꼭 return을 해서 save해줘야 함.
    	return customerRepository.save(customer)
    		.doOnNext((c) -> {
    			EmitResult result = sink.tryEmitNext(c.toString());
    	        if (result.isFailure()) { 
    	            System.out.println("이벤트 전송중 오류가 났습니다. ");
    	          }
    		});
    }
}
