/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package samples.websocket.jetty;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import samples.websocket.jetty.client.GreetingService;
import samples.websocket.jetty.client.SimpleClientWebSocketHandler;
import samples.websocket.jetty.client.SimpleGreetingService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(SampleJettyWebSocketsApplication.class)
@WebIntegrationTest({"server.port=0"})
@DirtiesContext
public class SampleWebSocketsApplicationTests {

	private static Log logger = LogFactory.getLog(SampleWebSocketsApplicationTests.class);

	@Value("${local.server.port}")
	private int port = 1234;

	@Test
	public void echoEndpoint() throws Exception {
		logger.info("Running the echoEndpoint test. Port: " + port + ". Path: /echo/websocket");
		ConfigurableApplicationContext context = new SpringApplicationBuilder(
				ClientConfiguration.class, PropertyPlaceholderAutoConfiguration.class)
						.properties("websocket.uri:ws://localhost:" + this.port
								+ "/echo/websocket")
						.run("--spring.main.web_environment=false");
		long count = context.getBean(ClientConfiguration.class).latch.getCount();
		AtomicReference<String> messagePayloadReference = context
				.getBean(ClientConfiguration.class).messagePayload;
		context.close();
		assertThat(count).isEqualTo(0);
		assertThat(messagePayloadReference.get())
				.isEqualTo("Did you say \"Hello world!\"?");
	}

	@Configuration
	static class ClientConfiguration implements CommandLineRunner {

		@Value("${websocket.uri}")	// --- If the applic'n class uses @ComponentScan this breaks ---
		private String webSocketUri;

		private final CountDownLatch latch = new CountDownLatch(1);

		private final AtomicReference<String> messagePayload = new AtomicReference<String>();

		@Override
		public void run(String... args) throws Exception {
			logger.info("Waiting for response: latch=" + this.latch.getCount());
			if (this.latch.await(10, TimeUnit.SECONDS)) {
				logger.info("Got response: " + this.messagePayload.get());
				System.out.println("Got response: latch=" + this.latch.getCount());
			}
			else {
				logger.info("Response not received: latch=" + this.latch.getCount());
				System.out.println("Response not received: latch=" + this.latch.getCount());
			}
		}

		@Bean
		public WebSocketConnectionManager wsConnectionManager() {

			logger.info("Setting up SimpleClientWebSocketHandler...");
			WebSocketConnectionManager manager = new WebSocketConnectionManager(client(),
					handler(), this.webSocketUri);
			manager.setAutoStartup(true);

			return manager;
		}

		@Bean
		public StandardWebSocketClient client() {
			return new StandardWebSocketClient();
		}

		@Bean
		public SimpleClientWebSocketHandler handler() {
			logger.info("Creating new SimpleClientWebSocketHandler using SimpleGreetingService...");
			return new SimpleClientWebSocketHandler(greetingService(), this.latch,
					this.messagePayload);
		}

		@Bean
		public GreetingService greetingService() {
			return new SimpleGreetingService();
		}
	}

}
