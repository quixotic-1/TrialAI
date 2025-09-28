package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.prompts.PromptEngineering;
import nz.ac.auckland.se206.services.TimerService;
import nz.ac.auckland.se206.utils.ChatHistoryUtil;

public class DefendantMemoryMechanismController {

  // static fields
  private static boolean introSent = false;

  // static methods
  public static void toggleIntroSentFalse() {
    introSent = false;
  }

  /** Clears all chat histories for the predefined participants. */
  public static void clearAllChatHistories() {
    String[] participants = {"AI-Defendant", "GREGOR", "K2"};
    for (String participant : participants) {
      String filename = "target/chat_history_" + participant + ".txt";
      try {
        Files.deleteIfExists(Paths.get(filename));
      } catch (IOException e) {
        // Ignore if file doesn't exist
      }
    }
    introSent = false;
  }

  // instance fields
  private final String[] images = {
    "/images/LogoBase1.png",
    "/images/LogoBase2.png",
    "/images/LogoBase3.png",
    "/images/LogoOG.png",
    "/images/LogoBase4.png"
  };

  // fxml fields
  @FXML private Label lblTimer;
  @FXML private Pagination paginationLogos;
  @FXML private ImageView imgComputer;
  @FXML private TextArea txtChat;
  @FXML private TextField txtField;
  @FXML private Label lblLogoBase;
  @FXML private Label lblRegistered;
  @FXML private Button btnSend;
  @FXML private Label lblTitle;
  @FXML private Button btnBack;

  // instance fields
  private ChatCompletionRequest chatCompletionRequest;
  private String profession;
  private List<ChatMessage> chatHistory = new ArrayList<>();
  private boolean logoFound = false;

  // Single-threaded pool for network I/O
  private final ExecutorService chatPool =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "chat-io");
            t.setDaemon(true);
            return t;
          });

  // public methods
  /**
   * Sets the profession for the chat context and initializes the ChatCompletionRequest.
   *
   * @param profession the profession to set
   */
  public void setProfession(String who) {
    this.profession =
        (who != null && !who.isEmpty()) ? who : "AI-Defendant"; // Default to "AI-Defendant"
    try {
      chatCompletionRequest =
          new ChatCompletionRequest(ApiProxyConfig.readConfig())
              .setModel(ChatCompletionRequest.Model.GPT_4_1_MINI)
              .setMaxTokens(200)
              .setTemperature(0.7)
              .setTopP(1.0)
              .setN(1); // Always 1 response
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    String sys;
    try {
      sys = getSystemPrompt(); // Load system prompt from file
    } catch (Exception ex) {
      ex.printStackTrace();
      sys =
          "You are "
              + this.profession
              + " in 'The Trial of AI-Defendant'. Stay in role."; // Fallback
    }
    chatCompletionRequest.addMessage(new ChatMessage("system", sys)); // Add system prompt

    // Load previous chat history if it exists
    loadChatHistory();
  }

  /** Clears all chat content from the chat display area. */
  public void clearChat() {
    txtChat.clear();
  }

  /**
   * Initializes the controller by setting up UI styles, timer bindings, pagination controls, and
   * handling the initial introduction sequence for the AI defendant.
   */
  public void initialize() {
    // sets the white background of the timer and title label
    lblTitle.setStyle("-fx-background-color: white; -fx-padding: 5px;");
    lblTimer.setStyle("-fx-background-color: white; -fx-padding: 5px;");

    // Bind lblTimer to TimerService
    if (lblTimer != null) {
      lblTimer
          .textProperty()
          .bind(TimerService.getInstance(App.getContext()).timeLeftTextBinding());
    }

    createPagination();
    setProfession("AI-Defendant");

    // Only run the intro sequence the very first time
    if (!introSent) {
      introSent = true;
      showThinkingMessage();
      ChatMessage introMsg =
          new ChatMessage(
              "system",
              "Introduce yourself as Logo Novo, an AI on trial for copying a logo. Tell the user to"
                  + " look around the room for clues. Keep this to 1 sentence. ");
      runGptAsync(introMsg);
    }
  }

  /**
   * Creates and configures the pagination control for displaying logo images. Sets up page count
   * and factory for displaying different logo images with appropriate labels.
   */
  public void createPagination() {
    // creates the paginiation which is my interactable element for the AI defendant
    paginationLogos.setPageCount(images.length);
    paginationLogos.setPageFactory(
        new Callback<Integer, Node>() {
          @Override
          public ImageView call(Integer pageIndex) {
            if (pageIndex >= images.length) {
              return null;
            }
            // will tell send a function to tell the GPT that the user has found the interactable
            // element
            if (pageIndex == 1 && !logoFound) {
              logoFound = true;
              addImageFound();
            }
            if (pageIndex == 3) {
              // changes the label to show these logos are registered, and the author they are
              // registered to
              lblRegistered.setText("Unregistered - author unknown");
            } else if (pageIndex == 0 || pageIndex == 4) {
              lblRegistered.setText("Registered - RoboCorp™ ");
            } else {
              // changes the label to show this logo is unregistered.
              lblRegistered.setText("Registered - CreatureGames™ ");
            }

            ImageView imageView =
                new ImageView(new Image(getClass().getResourceAsStream(images[pageIndex])));
            imageView.setFitWidth(361);
            imageView.setFitHeight(180);
            imageView.setPreserveRatio(true);
            return imageView;
          }
        });
  }

  /**
   * Notifies the AI that the user has discovered the logo evidence. Sends a system message to
   * trigger the appropriate AI response when evidence is found.
   */
  public void addImageFound() {
    // updating the chatgpt, letting it know that the user has found the logo they were looking at.
    showThinkingMessage();
    ChatMessage systemUpdate =
        new ChatMessage(
            "system",
            "Say exactly this: You've found the website I was on! That's where I found the logo."
                + " Keep this to 1 sentence.");
    chatCompletionRequest.addMessage(systemUpdate);
    runGptAsync(systemUpdate);
  }

  // private methods
  /** Saves the chat history between the user and the AI defendant (Logo Nova) */
  private void saveChatHistory() {
    StringBuilder sb = new StringBuilder(); // StringBuilder for efficient string concatenation
    for (ChatMessage msg : chatHistory) {
      sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
    }
    try {
      Files.write(
          Paths.get("target/chat_history_" + profession + ".txt"), // filename
          sb.toString().getBytes(StandardCharsets.UTF_8)); // file contents
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /** Loads the chat history back in for the AI Defendant, if the user had already spoken with it */
  private void loadChatHistory() {
    chatHistory.clear();
    txtChat.clear();
    String filename = "target/chat_history_" + profession + ".txt";
    try {
      List<String> lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
      for (String line : lines) {
        // Simple parsing: "role: message"
        int sep = line.indexOf(": ");
        if (sep > 0) {
          String role = line.substring(0, sep);
          String content = line.substring(sep + 2);
          ChatMessage msg = new ChatMessage(role, content);
          chatHistory.add(msg);
          txtChat.appendText(role + ": " + content + "\n\n");
        }
      }
    } catch (IOException e) {
      // No previous chat history, or error reading file; ignore
    }
  }

  private void runGptAsync(ChatMessage msg) {
    CompletableFuture.supplyAsync(
        () -> {
          try {
            return runGpt(msg); // run blocking network call OFF the FX thread
          } catch (ApiProxyException e) {
            throw new RuntimeException(e);
          }
        },
        chatPool);
  }

  private String getSystemPrompt() {
    Map<String, String> map = new HashMap<>();
    return PromptEngineering.getPrompt("defendant.txt", map);
  }

  /**
   * Appends a chat message to the chat text area.
   *
   * @param msg the chat message to append
   */
  private void appendChatMessage(ChatMessage msg) {
    // Modify the displayed role for assistant messages
    String displayRole = msg.getRole();
    if ("assistant".equals(displayRole)) {
      displayRole = "Logo Nova";
    }

    // Use the modified display role for UI text
    txtChat.appendText(displayRole + ": " + msg.getContent() + "\n\n");
    // Still save the original message with correct role for API communication
    chatHistory.add(msg);
  }

  /**
   * Runs the GPT model with a given chat message.
   *
   * @param msg the chat message to process
   * @return the response chat message
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  private ChatMessage runGpt(ChatMessage msg) throws ApiProxyException {

    chatCompletionRequest.addMessage(msg);

    // If this is a user message, enhance it with context from other conversations
    ChatMessage messageToSend = msg;
    if ("user".equals(msg.getRole())) {
      String otherHistories = ChatHistoryUtil.getOtherChatHistories("AI-Defendant");
      if (!otherHistories.isEmpty()) {
        String enhancedContent =
            msg.getContent()
                + "\n\n"
                + "[CONVERSATION HISTORY REFERENCE: The user has spoken to other participants. When"
                + " the user asks 'what did I say to [PARTICIPANT]', look for the exact"
                + " conversation section with that participant and find messages that start with"
                + " 'USER (to PARTICIPANT):'. Do NOT mix up conversations between different"
                + " participants. Each conversation is clearly separated.\n\n"
                + otherHistories
                + "IMPORTANT: If asked about what the user said to a specific participant, ONLY"
                + " look in that participant's conversation section. Do not reference other"
                + " participants' conversations when answering about a specific one.]";
        messageToSend = new ChatMessage(msg.getRole(), enhancedContent);
      }
    }

    chatCompletionRequest.addMessage(messageToSend);

    try {
      ChatCompletionResult chatCompletionResult = chatCompletionRequest.execute();
      Choice result = chatCompletionResult.getChoices().iterator().next();
      ChatMessage reply = result.getChatMessage();

      if (reply == null || reply.getContent() == null || reply.getContent().trim().isEmpty()) {
        return null;
      }
      chatCompletionRequest.addMessage(reply);
      Platform.runLater(
          () -> {
            String currentText = txtChat.getText();
            // Remove *all* "Logo Nova is thinking..." lines, no matter where they are
            currentText = currentText.replace("Logo Nova is thinking...\n", "");
            txtChat.setText(currentText);
            appendChatMessage(reply);
          });
      return reply;

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Sends a message to the GPT model.
   *
   * @param event the action event triggered by the send button
   * @throws ApiProxyException if there is an error communicating with the API proxy
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void onSendMessage(ActionEvent event) throws ApiProxyException, IOException {
    String message = txtField.getText().trim();
    if (message.isEmpty()) {
      return;
    }
    txtField.clear();
    ChatMessage msg = new ChatMessage("user", message);
    appendChatMessage(msg);
    showThinkingMessage(); // show thinking while GPT replies
    runGptAsync(msg);
  }

  private void showThinkingMessage() {
    // this is what will put the Logo Nova thinking message into the text area.
    Platform.runLater(() -> txtChat.appendText("Logo Nova is thinking...\n"));
  }

  /**
   * Navigates back to the previous view.
   *
   * @param event the action event triggered by the go back button
   * @throws ApiProxyException if there is an error communicating with the API proxy
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void onGoBack(ActionEvent event) throws ApiProxyException, IOException {
    System.out.println("defendantMemoryMechanismController: onGoBack called");
    saveChatHistory();
    System.out.println("defendantMemoryMechanismController: Calling App.setRoot('room')");
    App.setRoot("room");
  }
}
