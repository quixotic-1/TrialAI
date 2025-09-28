package nz.ac.auckland.apiproxy.chat.openai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatCompletionResult {

  private String model;
  private long created;

  private int usagePromptToken;
  private int usageCompletionTokens;
  private int usageTotalTokens;

  private List<Choice> choices = new ArrayList<>();

  protected ChatCompletionResult(Map<String, Object> chatCompletion) {
    parse(chatCompletion);
  }

  private void parse(Map<String, Object> chatCompletion) {
    model = chatCompletion.get("model").toString();
    created =
        chatCompletion.get("created") == null
            ? 0
            : Long.parseLong(chatCompletion.get("created").toString());
    usagePromptToken = getUsage("prompt_tokens", chatCompletion);
    usageCompletionTokens = getUsage("completion_tokens", chatCompletion);
    usageTotalTokens = getUsage("total_tokens", chatCompletion);

    List<?> choicesJson = (List<?>) chatCompletion.get("choices");
    for (int c = 0; c < choicesJson.size(); c++) {
      Map<?, ?> choiceJson = (Map<?, ?>) choicesJson.get(c);
      Map<?, ?> messageJson = (Map<?, ?>) choiceJson.get("message");

      String role = messageJson.get("role").toString();
      String content = messageJson.get("content").toString();

      int index = Integer.parseInt(choiceJson.get("index").toString());
      String finishReason = choiceJson.get("finish_reason").toString();

      choices.add(new Choice(new ChatMessage(role, content), index, finishReason));
    }
  }

  public int getUsagePromptTokens() {
    return usagePromptToken;
  }

  public int getUsageCompletionTokens() {
    return usageCompletionTokens;
  }

  public int getUsageTotalTokens() {
    return usageTotalTokens;
  }

  public String getModel() {
    return model;
  }

  public long getCreated() {
    return created;
  }

  private int getUsage(String key, Map<String, Object> chatCompletion) {
    Map<?, ?> usage = (Map<?, ?>) chatCompletion.get("usage");
    return Integer.parseInt(usage.get(key).toString());
  }

  public Choice getChoice(int index) {
    if (index < 0 || index >= choices.size()) {
      throw new IllegalArgumentException(
          "Choice index out of bounds. Index: " + index + ", Choices: " + choices.size());
    }
    return choices.get(index);
  }

  public int getNumChoices() {
    return choices.size();
  }

  public Iterable<Choice> getChoices() {
    return choices;
  }
}
