package software.amazon.ram.permission;

import java.time.Duration;
import software.amazon.awssdk.services.ram.RamClient;
import software.amazon.awssdk.services.ram.model.CreatePermissionRequest;
import software.amazon.awssdk.services.ram.model.CreatePermissionResponse;
import software.amazon.awssdk.services.ram.model.ResourceSharePermissionSummary;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<RamClient> proxyClient;

    @Mock
    RamClient ramClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        ramClient = mock(RamClient.class);
        proxyClient = MOCK_PROXY(proxy, ramClient);
    }

    @AfterEach
    public void tear_down() {
        verify(ramClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(ramClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final CreateHandler handler = new CreateHandler();
        final ResourceModel model =
                ResourceModel.builder()
                        .name("test")
                        .resourceType("foo")
                        .policyTemplate(PermissionHelper.convertToJsonObject("{\"Effect\":\"Allow\",\"Action\":[\"foo:bar\"]}"))
                        .build();
        final CreatePermissionResponse createPermissionResponse = CreatePermissionResponse.builder()
                        .permission(ResourceSharePermissionSummary.builder().build()).build();
        when(proxyClient.client().createPermission(any(CreatePermissionRequest.class)))
                .thenReturn(createPermissionResponse);
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken("token")
                .logicalResourceIdentifier("testpermission")
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
