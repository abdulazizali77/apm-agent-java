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

import co.elastic.apm.agent.bci.ElasticApmAgent;
import co.elastic.apm.agent.bci.TracerAwareInstrumentation;
import co.elastic.apm.agent.http.client.HttpClientHelper;
import co.elastic.apm.agent.impl.ElasticApmTracer;
import co.elastic.apm.agent.impl.Scope;
import co.elastic.apm.agent.impl.Tracer;
import co.elastic.apm.agent.impl.context.SpanContext;
import co.elastic.apm.agent.impl.transaction.AbstractSpan;
import co.elastic.apm.agent.impl.transaction.Span;
import co.elastic.apm.agent.impl.transaction.TraceContext;
import co.elastic.apm.agent.impl.transaction.Transaction;
import co.elastic.apm.agent.sdk.advice.AssignTo;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.reactivestreams.Subscription;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;

import static net.bytebuddy.matcher.ElementMatchers.*;


public class WebfluxClientInstrumentation extends TracerAwareInstrumentation {

    @Override
    public String getAdviceClassName() {
        return "co.elastic.apm.agent.webflux.client.WebfluxClientInstrumentation$WebfluxClientExecuteAdvice";
    }

    public static class WebfluxClientExecuteAdvice {
        {
            System.out.println("WebfluxClientExecuteAdvice static");
        }

        private static Span activeSpan;

        //cant have two separate advice
        /*
        @Nonnull
        @AssignTo()
        @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
        public static Object[] onBeforeDoOnSignal(
            final @Advice.Argument(0) @Nullable Flux source,
            final @Advice.Argument(1) @Nullable Consumer<? super Subscription> onSubscribe,
            final @Advice.Argument(2) @Nullable Consumer onNext,
            final @Advice.Argument(3) @Nullable Consumer<? super Throwable> onError,
            final @Advice.Argument(4) @Nullable Runnable onComplete,
            final @Advice.Argument(5) @Nullable Runnable onAfterTerminate,
            final @Advice.Argument(6) @Nullable LongConsumer onRequest,
            final @Advice.Argument(7) @Nullable Runnable onCancel,
            @Advice.This Object thiz,
            @Advice.Origin Class<?> clazz,
            @Advice.Origin("#m") String methodName
        ) {
            System.out.println(methodName);
            return new Object[]{};
        }
        */


        @Nonnull
        @AssignTo()
        @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
        public static Object[] onBeforeRetrieve(
//            final @Advice.FieldValue("httpMethod") @Nullable HttpMethod httpMethod,
////            final @Advice.FieldValue("uri") @Nullable URI uri,
//            final @Advice.FieldValue("httpHeaders") @Nullable HttpHeaders httpHeaders,

//            final @Advice.FieldValue("statusHandlers") @Nullable List statusHandlers,
//            final @Advice.Argument(1) @Nullable Object arg1,

            //FIXME: unsure
            @Advice.This Object thiz,
            @Advice.Origin Class<?> clazz,
            @Advice.Origin("#m") String methodName
        ) {
            AbstractSpan<?> parent = tracer.getActive();
//            System.out.println("WebfluxClientExecuteAdvice onBeforeRetrieve " + methodName + " parent=" + parent);
            if (parent == null && activeSpan == null) {
                return null;
            }
            switch (methodName) {
                case "retrieve":
                    //FIXME
                    URI uri = URI.create("http://localhost/temperatures");
                    String hostName = "localhost";

                    //activeSpan = HttpClientHelper.startHttpClientSpan(parent, methodName, uri, hostName);
                    activeSpan = parent.createSpan();
//                    Transaction transaction = activeSpan.getTransaction();
//                    Scope scope = transaction.activateInScope();

                    if (activeSpan != null) {
                        activeSpan.activate();
                        SpanContext spanContext = activeSpan.getContext();
                        spanContext.addLabel("stream", 0);
                    }
                    return new Object[]{activeSpan};

                case "onComplete":
                    //FIXME:
                    if (thiz.getClass().getSimpleName().equals("TcpClientSubscriber")) {
                        if (!activeSpan.isFinished()) {
                            activeSpan.end();
                        }
                    }

                    return new Object[]{null};
                case "onNext":

                    SpanContext spanContext = activeSpan.getContext();
                    Object stream = spanContext.getLabel("stream");
                    Integer iteration = 1;
                    if (stream != null) {
                        iteration = ((Integer) stream) + 1;
                    }
                    System.out.println("iteration="+iteration);
                    spanContext.addLabel("stream", iteration);


                    return new Object[]{null};

            }



            /*
            if (activeSpan != null) {
                //FIXME:
                switch (methodName) {
                    case "onNext":
                        activeSpan.activateInScope();
                        activeSpan.addLabel("stream", 1);
                        break;
                    case "onComplete":
                        activeSpan.deactivate();
                        break;
                    case "bodyToFlux":
                        System.out.println(thiz);

                        break;
                    default:
                        break;
                }

            } else {
//                private
//                ((WebClient.RequestBodyUriSpec) thiz).;
//                ((DefaultWebClient.DefaultRequestBodyUriSpec) thiz).httpMethod
//                thiz.uri
//                    ((DefaultWebClient.DefaultRequestBodyUriSpec) thiz).uri

                URI uri = URI.create("http://localhost/temperatures");
                String hostName = "localhost";

                activeSpan = HttpClientHelper.startHttpClientSpan(parent, methodName, uri, hostName);
                activeSpan.activate();

            }*/


            return new Object[]{activeSpan};
        }

        @Advice.OnMethodExit(suppress = Throwable.class, inline = false)
        public static void onAfterEnqueue(@Advice.Enter @Nullable Object[] enter) {
//            System.out.println("onAfterEnqueue ");
            Span span = enter != null ? (Span) enter[0] : null;

            if (span != null) {
                Object stream = span.getContext().getLabel("stream");

                System.out.println("onAfterEnqueue deactivating span " + span + " activeSpan=" + tracer.getActiveSpan() + " " + tracer.getActiveExitSpan());
                try {
                    span.deactivate();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println("onAfterEnqueue deactivated span " + span + " activeSpan=" + tracer.getActiveSpan() + " " + tracer.getActiveExitSpan());
            }
        }
    }

    @Override
    public ElementMatcher<? super TypeDescription> getTypeMatcher() {
        //WebClient retrieve
        //WebClient exchange

        //WebClient.ResponseSpec bodyToFlux

        return
//            named("org.springframework.web.reactive.function.client.WebClient")
//            .or(named("org.springframework.web.reactive.function.client.DefaultWebClient"))

//            .or(named("org.springframework.web.reactive.function.client.DefaultWebClient$DefaultResponseSpec")) //bodyToFlux
//
            named("org.springframework.web.reactive.function.client.DefaultWebClient$DefaultRequestBodyUriSpec") //retrieve
//            .or(named("reactor.core.publisher.MonoFlatMapMany")) //doOnSignal

                //.or(isChildOf(org.reactivestreams.Subscriber.class.getClassLoader()))
//            .or(hasSuperType(named("org.reactivestreams.Subscription")))

//            .or(hasSuperType(named("org.springframework.web.reactive.function.client.WebClient$ResponseSpec")))//bodyToFlux
//            .or(hasSuperType(named("org.springframework.web.reactive.function.client.WebClient$RequestBodyUriSpec")))//retrieve
//            .or(hasSuperType(named("org.springframework.web.reactive.function.client.WebClient$RequestHeadersSpec")))
//
//            .or(hasSuperType(named("org.springframework.web.reactive.function.client.WebClient")))
                .or(hasSuperType(named("org.reactivestreams.Subscriber")))
//            .or(hasSuperType(named("reactor.core.publisher.Flux")))

            ;

//        org.springframework.web.reactive.function.client.

    }

    @Override
    public ElementMatcher<? super MethodDescription> getMethodMatcher() {

        return
            named("retrieve")
                .or(named("onNext"))
                .or(named("onComplete"))
//                .or(named("bodyToFlux"))
//                .or(named("doOnSignal"))

//            .or(named("onError"))
//            .or(named("retrieve"))
//            .or(named("onSubscribe"))

//            .or(named("request"))
//            .or(named("cancel"))
//            .or(named("methodInternal"))
            ;
    }

    @Override
    public Collection<String> getInstrumentationGroupNames() {
        return Arrays.asList("http-client", "webflux-client");
    }

//    @Deprecated
//    public boolean indyPlugin() {
//        return false;
//    }
}
