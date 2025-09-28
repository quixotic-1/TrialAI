package nz.ac.auckland.se206.gpt.openai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest.Model;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import org.junit.jupiter.api.Test;

public class ChatCompletionServiceTest {

  @Test
  public void testGptAuckland() throws ApiProxyException {
    ApiProxyConfig config = ApiProxyConfig.readConfig();

    ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest(config);

    chatCompletionRequest
        .addMessage("system", "You are a helpful assistant. Reply in less than 20 words.")
        .addMessage("user", "Where is New Zealand?")
        .addMessage("system", "New Zealand is a country located in the southwestern Pacific Ocean.")
        .addMessage("user", "What's one city there?");

    chatCompletionRequest.setN(1);
    chatCompletionRequest.setMaxTokens(300);
    chatCompletionRequest.setModel(Model.GPT_4_1_NANO);
    Set<String> results = new HashSet<>();
    try {
      ChatCompletionResult chatCompletionResult = chatCompletionRequest.execute();

      System.out.println("model: " + chatCompletionResult.getModel());
      System.out.println("created: " + chatCompletionResult.getCreated());
      System.out.println("usagePromptTokens: " + chatCompletionResult.getUsagePromptTokens());
      System.out.println(
          "usageCompletionTokens: " + chatCompletionResult.getUsageCompletionTokens());
      System.out.println("usageTotalTokens: " + chatCompletionResult.getUsageTotalTokens());
      System.out.println("Number of choices: " + chatCompletionResult.getNumChoices());

      for (Choice choice : chatCompletionResult.getChoices()) {
        int index = choice.getIndex();
        String finishReason = choice.getFinishReason();
        ChatMessage message = choice.getChatMessage();
        String role = message.getRole();
        String content = message.getContent();
        results.add(content);
        System.out.println("Choice #" + index + ":");
        System.out.println("\tfinishReason: " + finishReason);
        System.out.println("\trole: " + role);
        System.out.println("\tcontent: " + content);
      }

    } catch (ApiProxyException e) {
      e.printStackTrace();
    }
    boolean found = false;
    for (String s : results) {
      if (s.contains("Auckland")) {
        found = true;
        break;
      }
    }
    assertTrue(found);
  }
}
