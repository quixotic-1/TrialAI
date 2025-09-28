package nz.ac.auckland.apiproxy.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiProxyConfig {

  private String email = null;
  private String apiKey = null;

  private static ApiProxyConfig instance;

  private ApiProxyConfig() {}

  private ApiProxyConfig(String apiKey, String email) {
    this.apiKey = apiKey;
    this.email = email;
  }

  public String getApiKey() {
    return apiKey;
  }

  public String getEmail() {
    return email;
  }

  public static synchronized ApiProxyConfig readConfig() throws ApiProxyException {
    if (instance == null) {
      File file = new File("apiproxy.config");
      try {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        instance = objectMapper.readValue(file, ApiProxyConfig.class);
      } catch (Exception e) {
        e.printStackTrace();
        String message =
            "Unable to read "
                + file.getAbsolutePath()
                + ". Please check the file exists and is valid.";
        throw new ApiProxyException(message);
      }
    }
    return instance;
  }
}
