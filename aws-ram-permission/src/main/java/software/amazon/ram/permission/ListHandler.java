package software.amazon.ram.permission;

import software.amazon.awssdk.services.ram.RamClient;
import software.amazon.awssdk.services.ram.model.ListPermissionsRequest;
import software.amazon.awssdk.services.ram.model.ListPermissionsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.Optional;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RamClient> proxyClient,
        final Logger logger) {

        final ListPermissionsRequest listPermissionsRequest =
                Translator.translateToListPermissionsRequest(request.getNextToken(), Optional.empty());
        final ListPermissionsResponse listPermissionsResponse =
                proxy.injectCredentialsAndInvokeV2(listPermissionsRequest, proxyClient.client()::listPermissions);
        final List<ResourceModel> models = Translator.translateFromListPermissionsResponse(listPermissionsResponse);
        final String nextToken = listPermissionsResponse.nextToken();

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModels(models)
            .nextToken(nextToken)
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
