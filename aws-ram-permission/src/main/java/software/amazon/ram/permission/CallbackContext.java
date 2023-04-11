package software.amazon.ram.permission;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Data
@lombok.Builder
@lombok.EqualsAndHashCode(callSuper = true)
@JsonDeserialize(builder = CallbackContext.CallbackContextBuilder.class)
public class CallbackContext extends StdCallbackContext {

  @JsonPOJOBuilder(withPrefix = "")
  public static class CallbackContextBuilder {}
}
