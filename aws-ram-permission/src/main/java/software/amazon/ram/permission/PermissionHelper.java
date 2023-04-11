package software.amazon.ram.permission;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

import java.util.Map;

public class PermissionHelper {

  static Map<String,Object> convertToJsonObject(String jsonString) {
    final ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> attribute = null;

    if (jsonString != null) {
      try {
        attribute = objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {
        });
      } catch (JsonProcessingException e) {
        throw new CfnInvalidRequestException(e);
      }
    }

    return attribute;
  }

  static String convertJsonObjectToString(final Map<String,Object> objectMap) {
    final ObjectMapper objectMapper = new ObjectMapper();
    String val = "";
    if (objectMap != null) {
      try {
        val = objectMapper.writeValueAsString(objectMap);

      } catch(JsonProcessingException e) {
        throw new CfnInvalidRequestException(e);
      }
    }
    return val;
  }

  static String generatePermissionName(final ResourceHandlerRequest<ResourceModel> request) {
    return IdentifierUtils.generateResourceIdentifier(
            request.getLogicalResourceIdentifier(),
            request.getClientRequestToken(),
            Constants.NAME_MAX_LENGTH);
  }
}
