/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.cloudformation;

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.lambda.powertools.cloudformation.handlers.NoPhysicalResourceIdSetHandler;
import software.amazon.lambda.powertools.cloudformation.handlers.PhysicalResourceIdSetHandler;
import software.amazon.lambda.powertools.cloudformation.handlers.RuntimeExceptionThrownHandler;

@WireMockTest
public class CloudFormationIntegrationTest {

    public static final String PHYSICAL_RESOURCE_ID = UUID.randomUUID().toString();
    public static final String LOG_STREAM_NAME = "FakeLogStreamName";

    private static CloudFormationCustomResourceEvent updateEventWithPhysicalResourceId(int httpPort,
                                                                                       String physicalResourceId) {
        CloudFormationCustomResourceEvent.CloudFormationCustomResourceEventBuilder builder = baseEvent(httpPort);

        builder.withPhysicalResourceId(physicalResourceId);
        builder.withRequestType("Update");

        return builder.build();
    }

    private static CloudFormationCustomResourceEvent deleteEventWithPhysicalResourceId(int httpPort,
                                                                                       String physicalResourceId) {
        CloudFormationCustomResourceEvent.CloudFormationCustomResourceEventBuilder builder = baseEvent(httpPort);

        builder.withPhysicalResourceId(physicalResourceId);
        builder.withRequestType("Delete");

        return builder.build();
    }

    private static CloudFormationCustomResourceEvent.CloudFormationCustomResourceEventBuilder baseEvent(int httpPort) {
        CloudFormationCustomResourceEvent.CloudFormationCustomResourceEventBuilder builder =
                CloudFormationCustomResourceEvent.builder()
                        .withResponseUrl("http://localhost:" + httpPort + "/")
                        .withStackId("123")
                        .withRequestId("234")
                        .withLogicalResourceId("345");

        return builder;
    }

    @ParameterizedTest
    @ValueSource(strings = {"Update", "Delete"})
    void physicalResourceIdTakenFromRequestForUpdateOrDeleteWhenUserSpecifiesNull(String requestType,
                                                                                  WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(put("/").willReturn(ok()));

        NoPhysicalResourceIdSetHandler handler = new NoPhysicalResourceIdSetHandler();
        int httpPort = wmRuntimeInfo.getHttpPort();

        CloudFormationCustomResourceEvent event = baseEvent(httpPort)
                .withPhysicalResourceId(PHYSICAL_RESOURCE_ID)
                .withRequestType(requestType)
                .build();

        handler.handleRequest(event, new FakeContext());

        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'SUCCESS')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + PHYSICAL_RESOURCE_ID + "')]"))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"Update", "Delete"})
    void physicalResourceIdDoesNotChangeWhenRuntimeExceptionThrownWhenUpdatingOrDeleting(String requestType,
                                                                                         WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(put("/").willReturn(ok()));

        RuntimeExceptionThrownHandler handler = new RuntimeExceptionThrownHandler();
        int httpPort = wmRuntimeInfo.getHttpPort();

        CloudFormationCustomResourceEvent event = baseEvent(httpPort)
                .withPhysicalResourceId(PHYSICAL_RESOURCE_ID)
                .withRequestType(requestType)
                .build();

        handler.handleRequest(event, new FakeContext());

        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'FAILED')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + PHYSICAL_RESOURCE_ID + "')]"))
        );
    }

    @Test
    void runtimeExceptionThrownOnCreateSendsLogStreamNameAsPhysicalResourceId(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(put("/").willReturn(ok()));

        RuntimeExceptionThrownHandler handler = new RuntimeExceptionThrownHandler();
        CloudFormationCustomResourceEvent createEvent = baseEvent(wmRuntimeInfo.getHttpPort())
                .withRequestType("Create")
                .build();
        handler.handleRequest(createEvent, new FakeContext());

        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'FAILED')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + LOG_STREAM_NAME + "')]"))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"Update", "Delete"})
    void physicalResourceIdSetFromRequestOnUpdateOrDeleteWhenCustomerDoesntProvideAPhysicalResourceId(
            String requestType, WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(put("/").willReturn(ok()));

        NoPhysicalResourceIdSetHandler handler = new NoPhysicalResourceIdSetHandler();
        int httpPort = wmRuntimeInfo.getHttpPort();

        CloudFormationCustomResourceEvent event = baseEvent(httpPort)
                .withPhysicalResourceId(PHYSICAL_RESOURCE_ID)
                .withRequestType(requestType)
                .build();

        Response response = handler.handleRequest(event, new FakeContext());

        assertThat(response).isNotNull();
        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'SUCCESS')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + PHYSICAL_RESOURCE_ID + "')]"))
        );
    }

    @Test
    void createNewResourceBecausePhysicalResourceIdNotSetByCustomerOnCreate(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(put("/").willReturn(ok()));

        NoPhysicalResourceIdSetHandler handler = new NoPhysicalResourceIdSetHandler();
        CloudFormationCustomResourceEvent createEvent = baseEvent(wmRuntimeInfo.getHttpPort())
                .withRequestType("Create")
                .build();
        Response response = handler.handleRequest(createEvent, new FakeContext());

        assertThat(response).isNotNull();
        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'SUCCESS')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + LOG_STREAM_NAME + "')]"))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"Create", "Update", "Delete"})
    void physicalResourceIdReturnedFromSuccessToCloudformation(String requestType, WireMockRuntimeInfo wmRuntimeInfo) {

        String physicalResourceId = UUID.randomUUID().toString();

        PhysicalResourceIdSetHandler handler = new PhysicalResourceIdSetHandler(physicalResourceId, true);
        CloudFormationCustomResourceEvent createEvent = baseEvent(wmRuntimeInfo.getHttpPort())
                .withRequestType(requestType)
                .build();
        Response response = handler.handleRequest(createEvent, new FakeContext());

        assertThat(response).isNotNull();
        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'SUCCESS')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + physicalResourceId + "')]"))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"Create", "Update", "Delete"})
    void physicalResourceIdReturnedFromFailedToCloudformation(String requestType, WireMockRuntimeInfo wmRuntimeInfo) {

        String physicalResourceId = UUID.randomUUID().toString();

        PhysicalResourceIdSetHandler handler = new PhysicalResourceIdSetHandler(physicalResourceId, false);
        CloudFormationCustomResourceEvent createEvent = baseEvent(wmRuntimeInfo.getHttpPort())
                .withRequestType(requestType)
                .build();
        Response response = handler.handleRequest(createEvent, new FakeContext());

        assertThat(response).isNotNull();
        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'FAILED')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + physicalResourceId + "')]"))
        );
    }

    private static class FakeContext implements Context {
        @Override
        public String getAwsRequestId() {
            return null;
        }

        @Override
        public String getLogGroupName() {
            return null;
        }

        @Override
        public String getLogStreamName() {
            return LOG_STREAM_NAME;
        }

        @Override
        public String getFunctionName() {
            return null;
        }

        @Override
        public String getFunctionVersion() {
            return null;
        }

        @Override
        public String getInvokedFunctionArn() {
            return null;
        }

        @Override
        public CognitoIdentity getIdentity() {
            return null;
        }

        @Override
        public ClientContext getClientContext() {
            return null;
        }

        @Override
        public int getRemainingTimeInMillis() {
            return 0;
        }

        @Override
        public int getMemoryLimitInMB() {
            return 0;
        }

        @Override
        public LambdaLogger getLogger() {
            return null;
        }
    }
}
