package software.amazon.ram.permission;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.ram.RamClient;
import software.amazon.awssdk.services.ram.model.CreatePermissionVersionRequest;
import software.amazon.awssdk.services.ram.model.CreatePermissionVersionResponse;
import software.amazon.awssdk.services.ram.model.GetPermissionRequest;
import software.amazon.awssdk.services.ram.model.GetPermissionResponse;
import software.amazon.awssdk.services.ram.model.ListPermissionVersionsRequest;
import software.amazon.awssdk.services.ram.model.ListPermissionVersionsResponse;
import software.amazon.awssdk.services.ram.model.ListReplacePermissionAssociationsWorkRequest;
import software.amazon.awssdk.services.ram.model.ListReplacePermissionAssociationsWorkResponse;
import software.amazon.awssdk.services.ram.model.ReplacePermissionAssociationsRequest;
import software.amazon.awssdk.services.ram.model.ReplacePermissionAssociationsResponse;
import software.amazon.awssdk.services.ram.model.ReplacePermissionAssociationsWork;
import software.amazon.awssdk.services.ram.model.ReplacePermissionAssociationsWorkStatus;
import software.amazon.awssdk.services.ram.model.ResourceSharePermissionDetail;
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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

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
    }

    private List<software.amazon.awssdk.services.ram.model.Tag> convertTags(List<Tag> tags) {
        return tags.stream()
                .map(t -> software.amazon.awssdk.services.ram.model.Tag.builder().key(t.getKey()).value(t.getValue()).build())
                .collect(Collectors.toList());
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final UpdateHandler handler = new UpdateHandler();
        final String name = "test";
        final String arn = "arn:aws:ram:us-east-1:123456789012:permission/" + name;
        final String resourceType = "foo";
        final String version = "1";
        final List<Tag> desiredTags = Collections.singletonList(Tag.builder().key("Key").value("Value").build());
        final Map<String, Object> perm = PermissionHelper.convertToJsonObject("{\"Effect\":\"Allow\",\"Action\":[\"foo:bar\"]}");

        final ResourceModel model = ResourceModel.builder()
                .arn(arn)
                .name(name)
                .resourceType(resourceType)
                .version(version)
                .policyTemplate(perm)
                .tags(desiredTags)
                .build();
        final ResourceModel prevModel = ResourceModel.builder()
                .arn(arn)
                .name(name)
                .resourceType(resourceType)
                .version(version)
                .policyTemplate(perm)
                .tags(Collections.emptyList())
                .build();
        when(proxyClient.client().getPermission(any(GetPermissionRequest.class)))
                .thenReturn(GetPermissionResponse.builder().permission(
                        ResourceSharePermissionDetail.builder()
                                .name(model.getName())
                                .arn(model.getArn())
                                .resourceType(model.getResourceType())
                                .status("ATTACHABLE")
                                .tags(convertTags(desiredTags))
                                .build()).build());
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model).previousResourceState(prevModel)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getTags().equals(desiredTags));
    }
}
