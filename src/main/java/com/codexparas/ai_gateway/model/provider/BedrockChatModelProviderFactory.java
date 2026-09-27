package com.codexparas.ai_gateway.model.provider;

import com.codexparas.ai_gateway.model.ProviderModelConfiguration;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

@Component
public final class BedrockChatModelProviderFactory
    extends AbstractSpringAiChatModelProviderFactory {

  public BedrockChatModelProviderFactory(ObjectProvider<BedrockProxyChatModel> models) {
    super("BEDROCK", models::getIfAvailable);
  }

  @Override
  public BedrockProxyChatModel create(ProviderModelConfiguration configuration) {
    String accessKey = configuration.authValue("accessKeyId", "access_key_id");
    String secretKey = configuration.authValue("secretAccessKey", "secret_access_key");
    var builder = BedrockProxyChatModel.builder();
    if (accessKey != null && secretKey != null) {
      builder.credentialsProvider(
          StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
    } else {
      builder.credentialsProvider(DefaultCredentialsProvider.create());
    }
    if (configuration.region() != null) builder.region(Region.of(configuration.region()));
    return builder
        .options(BedrockChatOptions.builder().model(configuration.model()).build())
        .build();
  }
}
