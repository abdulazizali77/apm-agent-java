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

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

@RestController
public class GroovyController {
    public GroovyController() {
        System.out.println("GroovyController");
    }

    @GetMapping("/hello")
    public String handleRequest(Model model) {
        model.addAttribute("message", "Message 123");
        return "hello";
    }

    @GetMapping(value = "/temperatures", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Integer> getTemperature() {
        System.out.println("/temperatures ");
        Random r = new Random();
        int low = 0;
        int high = 50;
        String[] stringArray = new String[]{"q", "w", "e", "r"};
        List<String> stringList = Arrays.asList(stringArray);

        Integer[] intArray = new Integer[]{5,6999,7};
//        List<Integer> intList = Arrays.asList(intArray);
//                return Flux.fromStream(Stream.generate(() -> r.nextInt(high - low) + low)
//                        .map(s -> String.valueOf(s))
//                        .peek((msg) -> {
//                            //logger.info(msg);
//                            System.out.println("msg");
//                        }))
//                        .map(s -> Integer.valueOf(s))
//                        .delayElements(Duration.ofSeconds(1));
        return Flux.fromArray(intArray);

    }
}
