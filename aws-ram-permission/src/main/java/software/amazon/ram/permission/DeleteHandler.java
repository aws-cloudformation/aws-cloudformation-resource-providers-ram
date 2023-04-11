package software.amazon.ram.permission;

import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ram.RamClient;
import software.amazon.awssdk.services.ram.model.DeletePermissionResponse;
import software.amazon.awssdk.services.ram.model.IdempotentParameterMismatchException;
import software.amazon.awssdk.services.ram.model.InvalidClientTokenException;
import software.amazon.awssdk.services.ram.model.MalformedArnException;
import software.amazon.awssdk.services.ram.model.OperationNotPermittedException;
import software.amazon.awssdk.services.ram.model.RamException;
import software.amazon.awssdk.services.ram.model.UnknownResourceException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RamClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        logger.log("DeleteHandler invoked in: "+ request.getStackId()+ " " +request.getLogicalResourceIdentifier());

        // DeleteHandler calls ram:DeletePermission to delete specified permission
        // The request will fail if permission does not exist
        // or any permission version is associated to resource shares outside of the template
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-RAM-Permission::Delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToDeletePermissionRequest)
                    .makeServiceCall((deletePermissionRequest, client) -> {
                        try {
                          final DeletePermissionResponse deletePermissionResponse = proxy.injectCredentialsAndInvokeV2(
                                  deletePermissionRequest,
                                  proxyClient.client()::deletePermission);
                          logger.log(String.format("%s %s successfully deleted.", ResourceModel.TYPE_NAME, request.getDesiredResourceState().getArn()));
                          return deletePermissionResponse;
                        } catch (UnknownResourceException e) {
                          throw new CfnNotFoundException(e);
                        } catch (OperationNotPermittedException
                                 | MalformedArnException
                                 | IdempotentParameterMismatchException
                                 | InvalidClientTokenException e) {
                          // when validatePermissionNotInUse fails
                          throw new CfnInvalidRequestException(e);
                        }  catch (RamException e) {
                          throw new CfnInternalFailureException(e);
                        } catch (final AwsServiceException e) {
                            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                        }
                    })
                    .progress()
            )
            .then(progress -> ProgressEvent.defaultSuccessHandler(request.getDesiredResourceState()));
    }
}
