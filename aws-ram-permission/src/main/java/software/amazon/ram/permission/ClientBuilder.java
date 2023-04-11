package software.amazon.ram.permission;

import software.amazon.awssdk.services.ram.RamClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {
  public static RamClient getClient() {
    return RamClient.builder()
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .build();
  }
}
