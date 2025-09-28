package nz.ac.auckland.apiproxy.tts;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.apiproxy.service.EndPoints;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class TextToSpeechRequest {

  public enum Provider {
    GOOGLE("google"),
    OPENAI("openai");

    private final String providerCode;

    Provider(String providerCode) {
      this.providerCode = providerCode;
    }

    public String getProviderCode() {
      return providerCode;
    }

    public Voice getDefaultVoice() {
      if (this == GOOGLE) {
        return Voice.GOOGLE_EN_US_STANDARD_I;
      } else {
        return Voice.OPENAI_NOVA;
      }
    }
  }

  public enum Voice {
    NOT_SET("not_set"),

    // OpenAI voices
    OPENAI_ALLOY("alloy"),
    OPENAI_ASH("ash"),
    OPENAI_CORAL("coral"),
    OPENAI_ECHO("echo"),
    OPENAI_FABLE("fable"),
    OPENAI_ONYX("onyx"),
    OPENAI_NOVA("nova"),
    OPENAI_SAGE("sage"),
    OPENAI_SHIMMER("shimmer"),

    // Google voices
    GOOGLE_EN_AU_NEURAL2_B("en-AU-Neural2-B"),
    GOOGLE_EN_AU_NEURAL2_C("en-AU-Neural2-C"),
    GOOGLE_EN_AU_STANDARD_A("en-AU-Standard-A"),
    GOOGLE_EN_AU_STANDARD_B("en-AU-Standard-B"),
    GOOGLE_EN_AU_STANDARD_C("en-AU-Standard-C"),
    GOOGLE_EN_AU_STANDARD_D("en-AU-Standard-D"),
    GOOGLE_EN_AU_WAVENET_B("en-AU-Wavenet-B"),
    GOOGLE_EN_AU_WAVENET_C("en-AU-Wavenet-C"),

    GOOGLE_EN_GB_NEURAL2_A("en-GB-Neural2-A"),
    GOOGLE_EN_GB_NEURAL2_B("en-GB-Neural2-B"),
    GOOGLE_EN_GB_STANDARD_B("en-GB-Standard-B"),
    GOOGLE_EN_GB_STANDARD_C("en-GB-Standard-C"),
    GOOGLE_EN_GB_STANDARD_D("en-GB-Standard-D"),
    GOOGLE_EN_GB_STANDARD_F("en-GB-Standard-F"),
    GOOGLE_EN_GB_WAVENET_B("en-GB-Wavenet-B"),
    GOOGLE_EN_GB_WAVENET_F("en-GB-Wavenet-F"),

    GOOGLE_EN_US_NEURAL2_A("en-US-Neural2-A"),
    GOOGLE_EN_US_NEURAL2_C("en-US-Neural2-C"),
    GOOGLE_EN_US_NEURAL2_D("en-US-Neural2-D"),
    GOOGLE_EN_US_NEURAL2_F("en-US-Neural2-F"),
    GOOGLE_EN_US_STANDARD_A("en-US-Standard-A"),
    GOOGLE_EN_US_STANDARD_C("en-US-Standard-C"),
    GOOGLE_EN_US_STANDARD_H("en-US-Standard-H"),
    GOOGLE_EN_US_STANDARD_I("en-US-Standard-I"),
    GOOGLE_EN_US_WAVENET_E("en-US-Wavenet-E"),
    GOOGLE_EN_US_WAVENET_J("en-US-Wavenet-J");

    private final String voiceCode;

    Voice(String voiceCode) {
      this.voiceCode = voiceCode;
    }

    public String getVoiceCode() {
      return voiceCode;
    }
  }

  private ApiProxyConfig config;

  private String text = null; // Required
  private Provider provider = Provider.OPENAI; // Default provider
  private Voice voice = Voice.NOT_SET;

  public TextToSpeechRequest(ApiProxyConfig config) {
    this.config = config;
  }

  public TextToSpeechRequest setText(String text) {
    this.text = text;
    return this;
  }

  public TextToSpeechRequest setProvider(Provider provider) {
    this.provider = provider;
    return this;
  }

  public TextToSpeechRequest setVoice(Voice voice) {
    this.voice = voice;
    return this;
  }

  @SuppressWarnings("resource")
  public TextToSpeechResult execute() throws ApiProxyException {

    if (isEmpty(text)) {
      throw new ApiProxyException("The text is missing or empty.");
    }

    if (provider == null) {
      provider = Provider.GOOGLE;
    }

    if (voice == null || voice == Voice.NOT_SET) {
      voice = provider.getDefaultVoice();
    }

    // Make sure the voice is supported by the provider
    String providerName = provider.name();
    String voiceName = voice.name();
    if (!voiceName.startsWith(providerName)) {
      throw new ApiProxyException(
          "The voice '"
              + voiceName
              + "' is not supported by the provider '"
              + providerName
              + "'. Please choose a different voice starting with '"
              + providerName
              + "_xxx'.");
    }

    try {
      JsonObjectBuilder jsonOverallBuilder =
          Json.createObjectBuilder() //
              .add("provider", provider.getProviderCode()) //
              .add("text", text);

      jsonOverallBuilder.add("voice", voice.getVoiceCode());
      jsonOverallBuilder.add("access_token", config.getApiKey()).add("email", config.getEmail());

      CloseableHttpClient client = HttpClients.createDefault();

      ResponseTtsViaProxy responseTts = null;
      JsonObject value = jsonOverallBuilder.build();

      HttpPost httpPost = new HttpPost(EndPoints.PROXY_TEXT_TO_SPEECH);
      httpPost.setHeader("Content-Type", "application/json");
      httpPost.setHeader("Accept", "application/json");
      httpPost.setEntity(new StringEntity(value.toString()));
      ObjectMapper mapperApiMapper = new ObjectMapper();

      responseTts =
          (ResponseTtsViaProxy)
              client.execute(
                  httpPost,
                  httpResponse ->
                      mapperApiMapper.readValue(
                          httpResponse.getEntity().getContent(), ResponseTtsViaProxy.class));

      if (!responseTts.success && responseTts.code != 0) {
        throw new ApiProxyException("Problem calling API: " + responseTts.message);
      }
      return new TextToSpeechResult(responseTts.audio);

    } catch (Exception e) {
      throw new ApiProxyException("Problem calling API: " + e.getMessage());
    }
  }

  private boolean isEmpty(String text) {
    return text == null || text.isEmpty();
  }
}
