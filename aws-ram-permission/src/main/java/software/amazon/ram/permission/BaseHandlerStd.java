package software.amazon.ram.permission;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ram.RamClient;
import software.amazon.awssdk.services.ram.model.GetPermissionRequest;
import software.amazon.awssdk.services.ram.model.GetPermissionResponse;
import software.amazon.awssdk.services.ram.model.InvalidNextTokenException;
import software.amazon.awssdk.services.ram.model.InvalidParameterException;
import software.amazon.awssdk.services.ram.model.MalformedArnException;
import software.amazon.awssdk.services.ram.model.UnknownResourceException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;

import java.time.Duration;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

  //For stabilization backoff, set the timeout to 2 mins and duration as 15 second for initial strategy
  public static final Constant BACKOFF_STRATEGY = Constant.of().timeout(Duration.ofMinutes(2L)).delay(Duration.ofSeconds(15L)).build();

  @Override
  public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
    final AmazonWebServicesClientProxy proxy,
    final ResourceHandlerRequest<ResourceModel> request,
    final CallbackContext callbackContext,
    final Logger logger) {
    final ProxyClient<RamClient> proxyClient = proxy.newProxy(ClientBuilder::getClient);
    return handleRequest(
      proxy,
      request,
      callbackContext != null ? callbackContext : new CallbackContext(),
      proxyClient,
      logger
    );
  }

  protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
    final AmazonWebServicesClientProxy proxy,
    final ResourceHandlerRequest<ResourceModel> request,
    final CallbackContext callbackContext,
    final ProxyClient<RamClient> proxyClient,
    final Logger logger);

  protected boolean isPermissionAlreadyExist(
          final ResourceHandlerRequest<ResourceModel> request,
          final ProxyClient<RamClient> proxyClient,
          final String permissionArn,
          final Logger logger) {
    try {
      getPermission(Translator.translateToGetPermissionRequest(request.getDesiredResourceState()), proxyClient);
      return true;
    } catch (CfnNotFoundException e) {
      return false;
    }
  }

  protected boolean isPermissionAssociationExist(
          final ResourceHandlerRequest<ResourceModel> request,
          final ProxyClient<RamClient> proxyClient) {
    try {
      return !proxyClient.injectCredentialsAndInvokeV2(
                      Translator.translateToListPermissionAssociationsRequest(request.getDesiredResourceState()),
                      proxyClient.client()::listPermissionAssociations).permissions().isEmpty();
    } catch (InvalidParameterException | MalformedArnException | InvalidNextTokenException e) {
      throw new CfnInvalidRequestException(e);
    } catch (final AwsServiceException e) {
      throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
    }
  }

  protected GetPermissionResponse getPermission(final GetPermissionRequest request,
                                                final ProxyClient<RamClient> proxyClient) {
    try {
      return proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::getPermission);
    } catch (UnknownResourceException e) {
      throw new CfnNotFoundException(e);
    } catch (InvalidParameterException | MalformedArnException e) {
      throw new CfnInvalidRequestException(e);
    } catch (SdkClientException e) {
      throw new CfnAccessDeniedException(e);
    } catch (Exception e) {
      throw new CfnInternalFailureException(e);
    }
  }
}
