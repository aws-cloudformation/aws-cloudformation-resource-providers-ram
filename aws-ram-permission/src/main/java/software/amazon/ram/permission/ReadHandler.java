package software.amazon.ram.permission;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ram.RamClient;
import software.amazon.awssdk.services.ram.model.GetPermissionResponse;
import software.amazon.awssdk.services.ram.model.InvalidParameterException;
import software.amazon.awssdk.services.ram.model.MalformedArnException;
import software.amazon.awssdk.services.ram.model.PermissionStatus;
import software.amazon.awssdk.services.ram.model.UnknownResourceException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.ram.permission.Translator.DELETED;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RamClient> proxyClient,
        final Logger logger) {
        this.logger = logger;
        return proxy.initiate("AWS-RAM-Permission::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
            .translateToServiceRequest(Translator::translateToGetPermissionRequest)
            .makeServiceCall((getPermissionRequest, client) -> {
                try {
                  final GetPermissionResponse getPermissionResponse =
                          proxyClient.injectCredentialsAndInvokeV2(
                                  getPermissionRequest,
                                  proxyClient.client()::getPermission);
                  logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                  if (getPermissionResponse.permission().status().equals(PermissionStatus.DELETED)) {
                      throw new CfnNotFoundException("AWS-RAM-Permission", request.getDesiredResourceState().getArn());
                  }
                  return getPermissionResponse;
                } catch (UnknownResourceException e) {
                  throw new CfnNotFoundException(e);
                } catch (CfnNotFoundException e) {
                  throw e;
                } catch (InvalidParameterException | MalformedArnException e) {
                  throw new CfnInvalidRequestException(e);
                } catch (SdkClientException e) {
                  throw new CfnAccessDeniedException(e);
                } catch (Exception e) {
                  throw new CfnInternalFailureException(e);
                }
            })
            .done(getPermissionResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromGetPermissionResponse(getPermissionResponse)));
    }
}
