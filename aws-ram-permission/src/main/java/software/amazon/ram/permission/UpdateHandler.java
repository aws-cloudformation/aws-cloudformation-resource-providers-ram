package software.amazon.ram.permission;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ram.RamClient;
import software.amazon.awssdk.services.ram.model.CreatePermissionVersionRequest;
import software.amazon.awssdk.services.ram.model.CreatePermissionVersionResponse;
import software.amazon.awssdk.services.ram.model.DeletePermissionVersionRequest;
import software.amazon.awssdk.services.ram.model.DeletePermissionVersionResponse;
import software.amazon.awssdk.services.ram.model.InvalidParameterException;
import software.amazon.awssdk.services.ram.model.TagResourceRequest;
import software.amazon.awssdk.services.ram.model.TagResourceResponse;
import software.amazon.awssdk.services.ram.model.UnknownResourceException;
import software.amazon.awssdk.services.ram.model.UntagResourceRequest;
import software.amazon.awssdk.services.ram.model.UntagResourceResponse;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnUnknownException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Map;
import java.util.Set;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<RamClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> updateResource(proxy, proxyClient, model, request, callbackContext, logger))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
    private ProgressEvent<ResourceModel, CallbackContext> updateResource(
            final AmazonWebServicesClientProxy proxy, final ProxyClient<RamClient> serviceClient,
            final ResourceModel resourceModel, final ResourceHandlerRequest<ResourceModel> handlerRequest,
            final CallbackContext callbackContext, final Logger logger) {
        logger.log(String.format("Cfn request: %s", handlerRequest));
        ProgressEvent<ResourceModel, CallbackContext> progess = ProgressEvent.progress(resourceModel, callbackContext);
        if (resourceModel.getArn() == null || resourceModel.getArn().length() == 0) {
            return progess;
        }
        if (TagHelper.shouldUpdateTags(resourceModel, handlerRequest)) {
            progess = progess
                    .then(p -> untagResource(proxy, serviceClient, resourceModel, handlerRequest, callbackContext, logger))
                    .then(p -> tagResource(proxy, serviceClient, resourceModel, handlerRequest, callbackContext, logger));
        } else {
            progess = ProgressEvent.success(resourceModel, callbackContext);
        }
        return progess;
    }

    private ProgressEvent<ResourceModel, CallbackContext> createNewVersion(final AmazonWebServicesClientProxy proxy, final ProxyClient<RamClient> serviceClient,
                                                                           final ResourceModel resourceModel, final ResourceHandlerRequest<ResourceModel> handlerRequest,
                                                                           final CallbackContext callbackContext, final Logger logger) {
        logger.log(String.format("Creating new version of permission: %s", resourceModel.getArn()));
        return proxy.initiate("AWS-RAM-Permission::CreatePermissionVersion", serviceClient, resourceModel, callbackContext)
                .translateToServiceRequest(model -> Translator.translateToCreatePermissionVersionRequest(resourceModel))
                .makeServiceCall(this::createPermVersionCall).progress();
    }

    private ProgressEvent<ResourceModel, CallbackContext> deleteOldVersion(final AmazonWebServicesClientProxy proxy, final ProxyClient<RamClient> serviceClient,
                                                                           final ResourceModel resourceModel, final ResourceHandlerRequest<ResourceModel> handlerRequest,
                                                                           final CallbackContext callbackContext, final Logger logger) {
        logger.log(String.format("Deleting old version %s of permission: %s", handlerRequest.getPreviousResourceState().getVersion(), resourceModel.getArn()));
        return proxy.initiate("AWS-RAM-Permission::DeletePermissionVersion", serviceClient, resourceModel, callbackContext)
                .translateToServiceRequest(model -> Translator.translateToDeletePermissionVersionRequest(resourceModel,
                        Integer.parseInt(handlerRequest.getPreviousResourceState().getVersion())))
                .makeServiceCall(this::deletePermVersionCall).progress();
    }

    private DeletePermissionVersionResponse deletePermVersionCall(final DeletePermissionVersionRequest req, final ProxyClient<RamClient> client) {
        try {
            return client.injectCredentialsAndInvokeV2(req, client.client()::deletePermissionVersion);
        } catch (final AwsServiceException e) {
            throw handleError(e, logger);
        }
    }

    private CreatePermissionVersionResponse createPermVersionCall(final CreatePermissionVersionRequest req, final ProxyClient<RamClient> client) {
        try {
            return client.injectCredentialsAndInvokeV2(req, client.client()::createPermissionVersion);
        } catch (final AwsServiceException e) {
            throw handleError(e, logger);
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> untagResource(final AmazonWebServicesClientProxy proxy, final ProxyClient<RamClient> serviceClient,
            final ResourceModel resourceModel, final ResourceHandlerRequest<ResourceModel> handlerRequest,
            final CallbackContext callbackContext, final Logger logger) {
        logger.log(String.format("Removing tags for arn: %s", resourceModel.getArn()));
        final Map<String, String> prevTags = TagHelper.getPreviouslyAttachedTags(handlerRequest);
        final Map<String, String> desiredTags = TagHelper.getNewDesiredTags(resourceModel, handlerRequest);
        final Set<String> tagsToRemove = TagHelper.generateTagsToRemove(prevTags, desiredTags);
        if (tagsToRemove.isEmpty()) {
            logger.log(String.format("No tags to remove"));
            return ProgressEvent.progress(resourceModel, callbackContext);
        }
        return proxy.initiate("AWS-RAM-Permission::UntagResource", serviceClient, resourceModel, callbackContext)
                .translateToServiceRequest(model -> Translator.untagResourceRequest(resourceModel, tagsToRemove))
                .makeServiceCall(this::untagResourceCall).progress();
    }

    private UntagResourceResponse untagResourceCall(final UntagResourceRequest request, final ProxyClient<RamClient> client) {
        try {
            logger.log(String.format("Untag Request: %s", request));
            UntagResourceResponse rsp = client.injectCredentialsAndInvokeV2(request, client.client()::untagResource);
            return rsp;
        } catch (final AwsServiceException e) {
            throw handleError(e, logger);
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> tagResource(final AmazonWebServicesClientProxy proxy, final ProxyClient<RamClient> serviceClient,
                                                                      final ResourceModel resourceModel, final ResourceHandlerRequest<ResourceModel> handlerRequest,
                                                                      final CallbackContext callbackContext, final Logger logger) {
        logger.log(String.format("Adding tags for arn: %s", resourceModel.getArn()));
        final Map<String, String> prevTags = TagHelper.getPreviouslyAttachedTags(handlerRequest);
        final Map<String, String> desiredTags = TagHelper.getNewDesiredTags(resourceModel, handlerRequest);
        final Map<String, String> tagsToAdd = TagHelper.generateTagsToAdd(prevTags, desiredTags);
        if (tagsToAdd.isEmpty()) {
            logger.log("No tags to add");
            return ProgressEvent.progress(resourceModel, callbackContext);
        }
        return proxy.initiate("AWS-RAM-Permission::TagResource", serviceClient, resourceModel, callbackContext)
                .translateToServiceRequest(model -> Translator.tagResourceRequest(resourceModel, tagsToAdd))
                .makeServiceCall(this::tagResourceCall).progress();
    }

    private TagResourceResponse tagResourceCall(final TagResourceRequest request, final ProxyClient<RamClient> client) {
        try {
            logger.log(String.format("Tag Request: %s", request));
            TagResourceResponse rsp = client.injectCredentialsAndInvokeV2(request, client.client()::tagResource);
            return rsp;
        } catch (final AwsServiceException e) {
            throw handleError(e, logger);
        }
    }

    private BaseHandlerException handleError(final Exception e, final Logger log) {
        log.log(String.format("Received exception with message %s", e.getMessage()));
        if (e instanceof InvalidParameterException) {
            return new CfnInvalidRequestException(e);
        } else if (e instanceof UnknownResourceException) {
            return new CfnUnknownException(e);
        }
        return new CfnServiceInternalErrorException(e);
    }
}
