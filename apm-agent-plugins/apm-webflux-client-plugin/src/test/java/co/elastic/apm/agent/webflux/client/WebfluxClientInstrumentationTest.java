/*-
 * #%L
 * Elastic APM Java agent
 * %%
 * Copyright (C) 2018 - 2020 Elastic and contributors
 * %%
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * #L%
 */
package co.elastic.apm.agent.webflux.client;


import co.elastic.apm.agent.AbstractInstrumentationTest;

import co.elastic.apm.agent.impl.TextHeaderMapAccessor;
import co.elastic.apm.agent.impl.context.Destination;
import co.elastic.apm.agent.impl.context.Http;
import co.elastic.apm.agent.impl.context.SpanContext;
import co.elastic.apm.agent.impl.error.ErrorCapture;
import co.elastic.apm.agent.impl.transaction.Outcome;
import co.elastic.apm.agent.impl.transaction.Span;
import co.elastic.apm.agent.impl.transaction.TraceContext;
import co.elastic.apm.agent.impl.transaction.Transaction;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebFlux;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;


//FIXME: why set the RANDOM_PORT here when wiremock is using its own random port?
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ContextConfiguration(initializers = {WireMockInitializer.class})
@ContextConfiguration(classes = {GroovyController.class})
@AutoConfigureWebMvc
@AutoConfigureWebFlux
@AutoConfigureWebTestClient
@EnableWebMvc
@EnableAutoConfiguration

public class WebfluxClientInstrumentationTest extends AbstractInstrumentationTest {

//    @Rule
//    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort(), false);

    @LocalServerPort
    private Integer port;

    //private OkHttpClient client;
    private WebClient client;
    private WebClient webClient;
//
//    @Autowired
//    private WireMockServer wireMockServer;

    @Before
    public void setupTests() {
        System.out.println("setupTests");
        startTestRootTransaction("parent of http span");
    }

    @After
    public final void after() {
        Transaction transaction = tracer.currentTransaction();
        assertThat(transaction).isNotNull();
        transaction.deactivate().end();
        assertThat(reporter.getTransactions()).hasSize(1);
    }

    @Test
    public void testBare() {
        System.out.println("junit4");
        List<Transaction> transactionList = reporter.getTransactions();
        List<Span> spanList = reporter.getSpans();
        List<ErrorCapture> errorCaptureList = reporter.getErrors();
        long dropped = reporter.getDropped();

        Span activeSpan = tracer.getActiveSpan();

    }

    protected Span verifyHttpSpan(String host, String path, int status) {
        assertThat(reporter.getFirstSpan(500)).isNotNull();
        assertThat(reporter.getSpans()).hasSize(1);
        Span span = reporter.getSpans().get(0);

//        int port = wireMockRule.port();

        String baseUrl = String.format("http://%s:%d", host, port);

        Http httpContext = span.getContext().getHttp();

        assertThat(httpContext.getUrl()).isEqualTo(baseUrl + path);
        assertThat(httpContext.getStatusCode()).isEqualTo(status);

        assertThat(span.getOutcome()).isEqualTo(status <= 400 ? Outcome.SUCCESS : Outcome.FAILURE);

        assertThat(span.getType()).isEqualTo("external");
        assertThat(span.getSubtype()).isEqualTo("http");
        assertThat(span.getAction()).isNull();

        Destination destination = span.getContext().getDestination();
        int addressStartIndex = (host.startsWith("[")) ? 1 : 0;
        int addressEndIndex = (host.endsWith("]")) ? host.length() - 1 : host.length();
        assertThat(destination.getAddress().toString()).isEqualTo(host.substring(addressStartIndex, addressEndIndex));
        assertThat(destination.getPort()).isEqualTo(port);
        assertThat(destination.getService().getName().toString()).isEqualTo(baseUrl);
        assertThat(destination.getService().getResource().toString()).isEqualTo("%s:%d", host, port);
        assertThat(destination.getService().getType()).isEqualTo("external");

        verifyTraceContextHeaders(span, path);

        return span;
    }

    private void verifyTraceContextHeaders(Span span, String path) {
        Map<String, String> headerMap = new HashMap<>();
        span.propagateTraceContext(headerMap, TextHeaderMapAccessor.INSTANCE);
        assertThat(headerMap).isNotEmpty();
//        List<LoggedRequest> loggedRequests = wireMockRule.findAll(anyRequestedFor(urlPathEqualTo(path)));
//        assertThat(loggedRequests).isNotEmpty();
//        loggedRequests.forEach(request -> {
//            assertThat(TraceContext.containsTraceContextTextHeaders(request, new HeaderAccessor())).isTrue();
//            headerMap.forEach((key, value) -> assertThat(request.getHeader(key)).isEqualTo(value));
//        });
    }

    @Test
    public void testWithTestConfig() throws InterruptedException {
        System.out.println("testWithTestConfig " + port);

//        WebClient.create("http://localhost:" + port).
//            get()
//            .uri("/temperatures")
//            .accept(MediaType.TEXT_EVENT_STREAM)
//            .retrieve()
//            .bodyToFlux(String.class)
//            .map(s -> String.valueOf(s))
//            .subscribe(msg -> {
//                System.out.println(msg);
//
//            });
//        WebClient.ResponseSpec responseSpec = WebClient.create("http://localhost:" + port).
//            get()
//            .uri("/temperatures")
//            .accept(MediaType.TEXT_EVENT_STREAM)
//            .retrieve();
//        responseSpec.toBodilessEntity().doOnSuccess(r->{});
//        Mono<ClientResponse> clientResponseMono = WebClient.create("http://localhost:" + port).
//            get()
//            .uri("/temperatures")
//            .accept(MediaType.TEXT_EVENT_STREAM)
//            .exchange();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        WebClient.create("http://localhost:" + port).
            get()
            .uri("/temperatures")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(String.class)
//            .doOnSubscribe(s -> {
//                System.out.println("onSubscription sub="+ s);
//            })
//            .doOnRequest(l -> {
//                System.out.println("onRequest " + l);
//            })
            .doOnNext(s -> {
                System.out.println("onNext " + s);
            })
//            .doOnEach(s -> {
//                System.out.println("onEach " + s);
//            })
//            .doOnDiscard(String.class, s -> {
//                System.out.println("onDiscard " + s);
//            })
//            .doOnError(throwable -> {
//                throwable.printStackTrace();
//            })
//            .doOnCancel(() -> {
//                System.out.println("onCancel");
//            })
            .doOnComplete(() -> {
                System.out.println("onComplete");
                countDownLatch.countDown();
            })
//            .doOnTerminate(() -> {
//                System.out.println("onTerminate");
//            })
            .map(s -> String.valueOf(s))
            .subscribe(msg -> {
                System.out.println("second poop " + msg);
            });


        reporter.awaitSpanCount(1);


        //while(true){
        List<Transaction> transactionList = reporter.getTransactions();
        List<Span> spanList = reporter.getSpans();
        List<ErrorCapture> errorCaptureList = reporter.getErrors();
        long dropped = reporter.getDropped();

        Span activeSpan = tracer.getActiveSpan();
        System.out.println("while poop=" + activeSpan);
        countDownLatch.await();
        for (Span s : spanList) {
            SpanContext spanContext = s.getContext();
            Object stream = spanContext.getLabel("stream");
            if (stream != null) {
                Integer i = (Integer) stream;
                System.out.println("spanList poop" + s.toString() + " " + s.getOutcome() + " " + i);
            }
        }
        //}
    }

    @TestConfiguration
    public static class TestCfg {
        static {
            System.out.print("TestCfg static init");
        }

        @RestController
        //@RequestMapping("/test")
        public class TestApi {

            @GetMapping("/hello")
            public String hello() {
                return "Hello World!";
            }

            @GetMapping(value = "/temperatures2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
            public Flux<Integer> getTemperature() {
                Random r = new Random();
                int low = 0;
                int high = 50;
                String[] stringArray = new String[]{"q", "w", "e", "r"};
                List<String> stringList = Arrays.asList(stringArray);

                Integer[] intArray = new Integer[]{3, 4, 1, 2, 6, 6, 6};
                List<Integer> intList = Arrays.asList(intArray);
//                return Flux.fromStream(Stream.generate(() -> r.nextInt(high - low) + low)
//                        .map(s -> String.valueOf(s))
//                        .peek((msg) -> {
//                            //logger.info(msg);
//                            System.out.println(msg);
//                        }))
//                        .map(s -> Integer.valueOf(s))
//                        .delayElements(Duration.ofSeconds(1));
                return Flux.fromArray(intArray);

            }
        }
    }

    //@BeforeEach
    public final void setUpWiremock() {
//        this.wireMockServer.stubFor(
//            WireMock.get("/todos2")
//                .willReturn(aResponse()
//                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
//                    .withBody("[{\"userId\": 1,\"id\": 1,\"title\": \"Learn Spring Boot 3.0\", \"completed\": false}," +
//                        "{\"userId\": 1,\"id\": 2,\"title\": \"Learn WireMock\", \"completed\": true}]"))
//        );
        List<Integer> list = new ArrayList();
        list.add(3);
        list.add(4);
        list.add(1);
        list.add(2);
        List<String> list2 = new ArrayList();
        list2.add("q");
        list2.add("wee");
        list2.add("vv");
        //FIXME: currently unused
        Flux<Integer> numberFlux = Flux.fromIterable(list);
        Flux<String> stringFlux = Flux.fromIterable(list2);


//        ResponseDefinitionBuilder rdb1 = aResponse()
//            // old spring 3.0 require content type
//            .withHeader("Content-Type", "text/plain")
//            .withBody("[3,4,1,2]");
//        ResponseDefinitionBuilder rdb2 = aResponse()
//            // old spring 3.0 require content type
//            .withHeader("Content-Type", "text/plain")
//            .withBody("[q,w,e,r,t]");
        // ensure that HTTP spans outcome is not unknown
//        wireMockRule.stubFor(any(urlEqualTo("/numbers"))
//            .willReturn(rdb1.withStatus(200)));
//        wireMockRule.stubFor(any(urlEqualTo("/strings"))
//            .willReturn(rdb2.withStatus(200)));

//        wireMockRule.stubFor(get(urlEqualTo("/error"))
//            .willReturn(dummyResponse()
//                .withStatus(515)));
//        wireMockRule.stubFor(get(urlEqualTo("/redirect"))
//            .willReturn(seeOther("/")));
//        wireMockRule.stubFor(get(urlEqualTo("/circular-redirect"))
//            .willReturn(seeOther("/circular-redirect")));

        startTestRootTransaction("parent of http span");
    }

/*
    @Test
    public void testGet(){
        //setup wiremockserver
        this.wireMockServer.stubFor(
            WireMock.get("/todos2")
                .willReturn(aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody("[{\"userId\": 1,\"id\": 1,\"title\": \"Learn Spring Boot 3.0\", \"completed\": false}," +
                        "{\"userId\": 1,\"id\": 2,\"title\": \"Learn WireMock\", \"completed\": true}]"))
        );
        startTestRootTransaction("parent of http span");
        client = WebClient.create();
        try {
            WebClient.ResponseSpec responseSpec = client.get()
                .uri(wireMockServer.baseUrl() + "/todos")
                .retrieve();
            Flux<String> result = responseSpec.bodyToFlux(String.class);

            result.toStream().forEach(s-> System.out.println("poop= "+s));
            //countDownLatch.await();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/


//
//    @Override
//    protected boolean isIpv6Supported() {
//
//        return false;
//    }
}

