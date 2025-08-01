/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.langchain4j.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.langchain4j.agent.pojos.TestFailingInputGuardrail;
import org.apache.camel.component.langchain4j.agent.pojos.TestJsonOutputGuardrail;
import org.apache.camel.component.langchain4j.agent.pojos.TestSuccessInputGuardrail;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "OPENAI_API_KEY", matches = ".*", disabledReason = "OpenAI API key required")
public class LangChain4jAgentGuardrailsIntegrationIT extends CamelTestSupport {

    protected ChatModel chatModel;
    private String openAiApiKey;

    @Override
    protected void setupResources() throws Exception {
        super.setupResources();

        openAiApiKey = System.getenv("OPENAI_API_KEY");
        if (openAiApiKey == null || openAiApiKey.trim().isEmpty()) {
            throw new IllegalStateException("OPENAI_API_KEY system property is required for testing");
        }
        chatModel = createModel();
    }

    protected ChatModel createModel() {
        return OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(GPT_4_O_MINI)
                .temperature(1.0)
                .timeout(ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @BeforeEach
    void setup() {
        // Reset all guardrails before each test
        TestSuccessInputGuardrail.reset();
        TestFailingInputGuardrail.reset();
        TestJsonOutputGuardrail.reset();
    }

    @Test
    void testAgentWithInputGuardrails() throws InterruptedException {

        MockEndpoint mockEndpoint = this.context.getEndpoint("mock:agent-response", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        String response = template.requestBody(
                "direct:agent-with-input-guardrails",
                "What is Apache Camel?",
                String.class);

        mockEndpoint.assertIsSatisfied();
        assertNotNull(response, "AI response should not be null");
        assertTrue(TestSuccessInputGuardrail.wasValidated(),
                "Input guardrail should have been called to validate the user message");
        assertTrue(response.toLowerCase().contains("camel") || response.toLowerCase().contains("integration"),
                "Response should contain information about Apache Camel");
    }

    @Test
    void testAgentWithMultipleInputGuardrails() throws InterruptedException {

        MockEndpoint mockEndpoint = this.context.getEndpoint("mock:agent-response", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        String response = template.requestBody(
                "direct:agent-with-multiple-input-guardrails",
                "What is integration?",
                String.class);

        mockEndpoint.assertIsSatisfied();
        assertNotNull(response, "AI response should not be null");
        assertTrue(TestSuccessInputGuardrail.wasValidated(),
                "First input guardrail should have been called");
        assertTrue(TestFailingInputGuardrail.wasValidated(),
                "Second input guardrail should have been called");
        assertEquals(1, TestFailingInputGuardrail.getCallCount(),
                "Second guardrail should have been called exactly once");
        assertTrue(response.toLowerCase().contains("integration") || response.toLowerCase().contains("connect"),
                "Response should contain information about integration");
    }

    @Test
    void testAgentWithJsonOutputGuardrail() throws InterruptedException {

        MockEndpoint mockEndpoint = this.context.getEndpoint("mock:agent-response", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        // Request a specific JSON format for person information
        String jsonRequest = """
                Please return information about a software engineer named John Doe in the following JSON format:
                {
                  "name": "string",
                  "profession": "string",
                  "experience": "number",
                  "skills": ["array", "of", "strings"]
                }
                """;

        String response = template.requestBody(
                "direct:agent-with-json-output-guardrail",
                jsonRequest,
                String.class);

        mockEndpoint.assertIsSatisfied();
        assertNotNull(response, "AI response should not be null");

        // Verify the output guardrail was called
        assertTrue(TestJsonOutputGuardrail.wasValidated(),
                "Output guardrail should have been called to validate the AI response");

        assertTrue(response.trim().startsWith("{"), "Response should start with JSON object");
        assertTrue(response.trim().endsWith("}"), "Response should end with JSON object");
        assertTrue(response.contains("\"name\""), "Response should contain name field");
        assertTrue(response.contains("\"profession\""), "Response should contain profession field");
        assertTrue(response.contains("John Doe"), "Response should contain the requested name");
    }

    @Test
    void testAgentWithJsonOutputGuardrailFailure() throws InterruptedException {
        MockEndpoint mockEndpoint = this.context.getEndpoint("mock:agent-response", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(0); // No message should reach the endpoint due to guardrail failure

        String nonJsonRequest = "Tell me a simple story about a cat in one sentence.";

        // Expect an exception due to guardrail failure
        Exception exception = assertThrows(Exception.class, () -> {
            template.requestBody(
                    "direct:agent-with-json-output-guardrail",
                    nonJsonRequest,
                    Object.class);
        });

        assertTrue(exception.getMessage().contains("Output validation failed") ||
                exception.getCause() != null && exception.getCause().getMessage().contains("Output validation failed"),
                "Exception should be related to output guardrail validation failure");

        assertTrue(TestJsonOutputGuardrail.wasValidated(),
                "Output guardrail should have been called to validate the AI response");

        mockEndpoint.assertIsSatisfied();
    }

    @Test
    void testAgentWithBothInputAndOutputGuardrails() throws InterruptedException {

        MockEndpoint mockEndpoint = this.context.getEndpoint("mock:agent-response", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        String jsonRequest = """
                Create a JSON profile for a data scientist named Alice Smith with 5 years of experience.
                Use this exact format:
                {
                  "name": "string",
                  "title": "string",
                  "yearsExperience": number,
                  "department": "string"
                }
                """;

        String response = template.requestBody(
                "direct:agent-with-mixed-guardrails",
                jsonRequest,
                String.class);

        mockEndpoint.assertIsSatisfied();
        assertNotNull(response, "AI response should not be null");

        assertTrue(TestSuccessInputGuardrail.wasValidated(),
                "Input guardrail should have been called to validate the user message");
        assertTrue(TestJsonOutputGuardrail.wasValidated(),
                "Output guardrail should have been called to validate the AI response");
        assertTrue(response.trim().startsWith("{"), "Response should start with JSON object");
        assertTrue(response.trim().endsWith("}"), "Response should end with JSON object");
        assertTrue(response.contains("\"name\""), "Response should contain name field");
        assertTrue(response.contains("Alice Smith"), "Response should contain the requested name");
        assertTrue(response.contains("\"title\"") || response.contains("\"department\""),
                "Response should contain professional information (title or department)");
        assertTrue(response.contains("\"yearsExperience\"") || response.contains("experience"),
                "Response should contain experience");
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        this.context.getRegistry().bind("chatModel", chatModel);

        return new RouteBuilder() {
            public void configure() {
                from("direct:agent-with-input-guardrails")
                        .to("langchain4j-agent:test-agent?chatModel=#chatModel&inputGuardrails=org.apache.camel.component.langchain4j.agent.pojos.TestSuccessInputGuardrail")
                        .to("mock:agent-response");

                from("direct:agent-with-multiple-input-guardrails")
                        .to("langchain4j-agent:test-agent?chatModel=#chatModel&inputGuardrails=org.apache.camel.component.langchain4j.agent.pojos.TestSuccessInputGuardrail,org.apache.camel.component.langchain4j.agent.pojos.TestFailingInputGuardrail")
                        .to("mock:agent-response");

                from("direct:agent-with-json-output-guardrail")
                        .to("langchain4j-agent:test-agent?chatModel=#chatModel&outputGuardrails=org.apache.camel.component.langchain4j.agent.pojos.TestJsonOutputGuardrail")
                        .to("mock:agent-response");

                from("direct:agent-with-mixed-guardrails")
                        .to("langchain4j-agent:test-agent?chatModel=#chatModel&inputGuardrails=org.apache.camel.component.langchain4j.agent.pojos.TestSuccessInputGuardrail&outputGuardrails=org.apache.camel.component.langchain4j.agent.pojos.TestJsonOutputGuardrail")
                        .to("mock:agent-response");
            }
        };
    }
}
