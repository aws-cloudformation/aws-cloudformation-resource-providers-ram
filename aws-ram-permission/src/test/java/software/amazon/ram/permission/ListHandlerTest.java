package software.amazon.ram.permission;

import org.junit.jupiter.api.AfterEach;
import software.amazon.awssdk.services.ram.RamClient;
import software.amazon.awssdk.services.ram.model.ListPermissionsRequest;
import software.amazon.awssdk.services.ram.model.ListPermissionsResponse;
import software.amazon.awssdk.services.ram.model.ResourceSharePermissionSummary;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<RamClient> proxyClient;

    @Mock
    RamClient client;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        client = mock(RamClient.class);
        proxyClient = MOCK_PROXY(proxy, client);
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(client);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ListHandler handler = new ListHandler();
        final ListPermissionsResponse listPermissionsResponse = ListPermissionsResponse.builder()
                .permissions(Collections.singletonList(ResourceSharePermissionSummary.builder().status("ATTACHABLE").build()))
                .nextToken(null)
                .build();

        when(proxyClient.client().listPermissions(any(ListPermissionsRequest.class)))
                .thenReturn(listPermissionsResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels().size()).isEqualTo(1);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).listPermissions(any(ListPermissionsRequest.class));
    }
}
