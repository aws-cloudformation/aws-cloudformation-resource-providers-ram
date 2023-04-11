package software.amazon.ram.permission;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ram.RamClient;
import software.amazon.awssdk.services.ram.model.CreatePermissionResponse;
import software.amazon.awssdk.services.ram.model.InvalidClientTokenException;
import software.amazon.awssdk.services.ram.model.InvalidParameterException;
import software.amazon.awssdk.services.ram.model.InvalidPolicyException;
import software.amazon.awssdk.services.ram.model.MalformedPolicyTemplateException;
import software.amazon.awssdk.services.ram.model.OperationNotPermittedException;
import software.amazon.awssdk.services.ram.model.PermissionAlreadyExistsException;
import software.amazon.awssdk.services.ram.model.PermissionLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.ram.permission.Constants.CREATE_PERMISSION_EXISTS_ALREADY_MESSAGE;

public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RamClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        logger.log("CreateHandler invoked in: "+ request.getStackId()+ " " +request.getLogicalResourceIdentifier());
        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-RAM-Permission::Create", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(desiredState ->
                            Translator.translateToCreatePermissionRequest(desiredState, TagHelper.getNewDesiredTags(desiredState, request), request.getClientRequestToken()))
                    .backoffDelay(BACKOFF_STRATEGY)
                    .makeServiceCall((createPermissionRequest, client) -> {
                      try {
                        final CreatePermissionResponse createPermissionResponse =
                                proxy.injectCredentialsAndInvokeV2(
                                        createPermissionRequest,
                                        proxyClient.client()::createPermission);
                        logger.log(String.format("%s %s successfully created.", ResourceModel.TYPE_NAME, createPermissionResponse.permission().arn()));
                        return createPermissionResponse;
                      } catch (PermissionAlreadyExistsException | OperationNotPermittedException e) {
                        if (e.getMessage().contains("already exists")) {
                          throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, model.getName(), e);
                        }
                        throw new CfnInvalidRequestException(e);
                      } catch (InvalidParameterException
                               | InvalidPolicyException
                               | MalformedPolicyTemplateException
                               | InvalidClientTokenException e) {
                        throw new CfnInvalidRequestException(e);
                      } catch (PermissionLimitExceededException e) {
                        throw new CfnServiceLimitExceededException(e);
                      } catch (final AwsServiceException e) {
                        throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                      }
                    })
                    .done(createPermissionResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromCreatePermissionResponse(createPermissionResponse))));
    }
}
