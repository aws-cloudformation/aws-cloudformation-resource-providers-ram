package software.amazon.ram.permission;

import software.amazon.awssdk.services.ram.model.CreatePermissionRequest;
import software.amazon.awssdk.services.ram.model.CreatePermissionResponse;
import software.amazon.awssdk.services.ram.model.CreatePermissionVersionRequest;
import software.amazon.awssdk.services.ram.model.CreatePermissionVersionResponse;
import software.amazon.awssdk.services.ram.model.DeletePermissionRequest;
import software.amazon.awssdk.services.ram.model.DeletePermissionVersionRequest;
import software.amazon.awssdk.services.ram.model.GetPermissionRequest;
import software.amazon.awssdk.services.ram.model.GetPermissionResponse;
import software.amazon.awssdk.services.ram.model.ListPermissionAssociationsRequest;
import software.amazon.awssdk.services.ram.model.ListPermissionVersionsRequest;
import software.amazon.awssdk.services.ram.model.ListReplacePermissionAssociationsWorkRequest;
import software.amazon.awssdk.services.ram.model.ReplacePermissionAssociationsRequest;
import software.amazon.awssdk.services.ram.model.Tag;
import software.amazon.awssdk.services.ram.model.ListPermissionsRequest;
import software.amazon.awssdk.services.ram.model.ListPermissionsResponse;
import software.amazon.awssdk.services.ram.model.ResourceSharePermissionDetail;
import software.amazon.awssdk.services.ram.model.ResourceSharePermissionSummary;
import software.amazon.awssdk.services.ram.model.TagResourceRequest;
import software.amazon.awssdk.services.ram.model.UntagResourceRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {
  static final String DELETED = "DELETED";

  static CreatePermissionVersionRequest translateToCreatePermissionVersionRequest(final ResourceModel model) {
    return CreatePermissionVersionRequest.builder()
            .permissionArn(model.getArn()).policyTemplate(PermissionHelper.convertJsonObjectToString(model.getPolicyTemplate())).build();
  }

  static CreatePermissionRequest translateToCreatePermissionRequest(final ResourceModel model, final Map<String, String> tags, final String clientToken) {
    return CreatePermissionRequest.builder()
            .name(model.getName())
            .resourceType(model.getResourceType())
            .policyTemplate(PermissionHelper.convertJsonObjectToString(model.getPolicyTemplate()))
            .tags(translateToTagsList(tags))
            .clientToken(clientToken)
            .build();
  }

  static ResourceModel translateFromCreatePermissionVersionResponse(final CreatePermissionVersionResponse response) {
    final ResourceSharePermissionDetail permission = response.permission();
    return ResourceModel.builder()
            .arn(permission.arn())
            .name(permission.name())
            .version(permission.version())
            .resourceType(permission.resourceType())
            .tags((permission.hasTags())
                    ? permission.tags().stream().map(tag -> software.amazon.ram.permission.Tag.builder()
                            .key(tag.key())
                            .value(tag.value())
                            .build())
                    .collect(Collectors.toList())
                    : null)
            .build();
  }

  static ResourceModel translateFromCreatePermissionResponse(final CreatePermissionResponse response) {
    final ResourceSharePermissionSummary permission = response.permission();
    return ResourceModel.builder()
            .arn(permission.arn())
            .name(permission.name())
            .version(permission.version())
            .resourceType(permission.resourceType())
            .tags((permission.hasTags())
                    ? permission.tags().stream().map(tag -> software.amazon.ram.permission.Tag.builder()
                            .key(tag.key())
                            .value(tag.value())
                            .build())
                    .collect(Collectors.toList())
                    : null)
            .build();
  }

  static DeletePermissionRequest translateToDeletePermissionRequest(final ResourceModel model) {
    return DeletePermissionRequest.builder()
            .permissionArn(model.getArn())
            .build();
  }

  static DeletePermissionVersionRequest translateToDeletePermissionVersionRequest(final ResourceModel model, final Integer oldVersion) {
    return DeletePermissionVersionRequest.builder()
            .permissionArn(model.getArn())
            .permissionVersion(oldVersion)
            .build();
  }

  static ReplacePermissionAssociationsRequest translateToReplacePermissionAssociationsRequest(final ResourceModel model) {
    return ReplacePermissionAssociationsRequest.builder()
            .fromPermissionArn(model.getArn())
            .toPermissionArn(model.getArn())
            .build();
  }

  static ListReplacePermissionAssociationsWorkRequest translateToListReplacePermissionAssociationsWorkRequest(final String workId) {
    return ListReplacePermissionAssociationsWorkRequest.builder()
            .workIds(workId)
            .build();
  }

  static ListPermissionVersionsRequest translateToListPermissionVersionsRequest(final ResourceModel model) {
    return ListPermissionVersionsRequest.builder()
            .permissionArn(model.getArn())
            .build();
  }

  static GetPermissionRequest translateToGetPermissionRequest(final ResourceModel model) {
    return GetPermissionRequest.builder()
            .permissionArn(model.getArn())
            .build();
  }

  static ResourceModel translateFromGetPermissionResponse(final GetPermissionResponse response) {
    final ResourceSharePermissionDetail permission = response.permission();
    return ResourceModel.builder()
            .arn(permission.arn())
            .name(permission.name())
            .version(permission.version())
            .resourceType(permission.resourceType())
            .isResourceTypeDefault(permission.isResourceTypeDefault())
            .permissionType(permission.permissionTypeAsString())
            .policyTemplate(PermissionHelper.convertToJsonObject(permission.permission()))
            .tags((permission.hasTags())
                    ? permission.tags().stream().map(tag ->
                            software.amazon.ram.permission.Tag.builder().key(tag.key()).value(tag.value()).build())
                            .collect(Collectors.toList())
                    : null)
            .build();
  }

  static ListPermissionsRequest translateToListPermissionsRequest(final String nextToken, final Optional<String> resourceType) {
    final ListPermissionsRequest.Builder builder = ListPermissionsRequest.builder().nextToken(nextToken);
    resourceType.ifPresent(builder::resourceType);
    return builder.build();
  }

  static List<ResourceModel> translateFromListPermissionsResponse(final ListPermissionsResponse response) {
    return streamOfOrEmpty(response.permissions())
            .filter(p -> !p.status().equals(DELETED))
            .map(permission -> ResourceModel.builder()
                    .arn(permission.arn())
                    .version(permission.version())
                    .isResourceTypeDefault(permission.isResourceTypeDefault())
                    .permissionType(permission.permissionTypeAsString())
                    .build())
            .collect(Collectors.toList());
  }

  static ListPermissionAssociationsRequest translateToListPermissionAssociationsRequest(final ResourceModel model) {
    return ListPermissionAssociationsRequest.builder().permissionArn(model.getArn()).build();
  }

  private static List<Tag> translateToTagsList(final Map<String, String> tags) {
    return tags == null ? null : tags.keySet().stream().map(key -> Tag.builder().key(key).value(tags.get(key)).build())
            .collect(Collectors.toList());

  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }

  static TagResourceRequest tagResourceRequest(final ResourceModel model, final Map<String, String> addedTags) {
    return TagResourceRequest.builder().tags(TagHelper.convertToSet(addedTags)).resourceArn(model.getArn()).build();
  }

  static UntagResourceRequest untagResourceRequest(final ResourceModel model, final Set<String> removedTags) {
    return UntagResourceRequest.builder().tagKeys(removedTags).resourceArn(model.getArn()).build();
  }
}
