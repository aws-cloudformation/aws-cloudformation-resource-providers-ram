package software.amazon.ram.permission;

import java.time.Duration;
import software.amazon.awssdk.services.ram.RamClient;
import software.amazon.awssdk.services.ram.model.DeletePermissionRequest;
import software.amazon.awssdk.services.ram.model.DeletePermissionResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

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
        final DeleteHandler handler = new DeleteHandler();

        final ResourceModel model =
                ResourceModel.builder()
                        .arn("arn:aws:ram:us-east-1:123456789012:permission/test")
                        .name("test")
                        .resourceType("foo")
                        .policyTemplate(PermissionHelper.convertToJsonObject("{\"Effect\":\"Allow\",\"Action\":[\"foo:bar\"]}"))
                        .build();
        when(proxyClient.client().deletePermission(any(DeletePermissionRequest.class)))
                .thenReturn(DeletePermissionResponse.builder().build());
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
