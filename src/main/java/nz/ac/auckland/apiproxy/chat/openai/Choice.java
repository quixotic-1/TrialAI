package nz.ac.auckland.apiproxy.chat.openai;

public class Choice {

  private ChatMessage message;
  private int index;
  private String finishReason;

  protected Choice(ChatMessage message, int index, String finishReason) {
    this.message = message;
    this.index = index;
    this.finishReason = finishReason;
  }

  public ChatMessage getChatMessage() {
    return message;
  }

  public String getFinishReason() {
    return finishReason;
  }

  public int getIndex() {
    return index;
  }
}
