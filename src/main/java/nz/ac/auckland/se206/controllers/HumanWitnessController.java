package nz.ac.auckland.se206.controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.ImageCursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest.Model;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.services.TimerService;
import nz.ac.auckland.se206.utils.ChatHistoryUtil;

public class HumanWitnessController {

  private static final String CHAT_PROMPT = "chat_humanWitness.txt";
  // these labels must match what you write to the file and the UI
  private static final String DISPLAY_USER = "[You]";
  private static final String DISPLAY_ASSISTANT = "Rentbrand Picosso";

  /**
   * Resets the static state by clearing all chat history and reveal comment files. This method
   * removes conversation files from both primary (target) and backup (se206) directories to ensure
   * clean state initialization for new game sessions. Used for game reset functionality and state
   * management.
   */
  public static void resetStaticState() {
    // Clear the chat history files from the correct locations
    try {
      // Clear from target directory (where the file is actually saved)
      Files.deleteIfExists(Paths.get("target/humanWitness_chatHistory.txt"));
      Files.deleteIfExists(Paths.get("target/humanWitness_revealComment.txt"));

      // Clear from se206 directory (backup location)
      Files.deleteIfExists(Paths.get("se206/humanWitness_chatHistory.txt"));
      Files.deleteIfExists(Paths.get("se206/humanWitness_revealComment.txt"));

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @FXML private ImageView imgHumanWitnessMainMenu;
  @FXML private Label timerLabel;

  @FXML private Button btnGoBack;
  @FXML private TextArea txtaChat;
  @FXML private TextField txtInput;
  @FXML private Button btnSend;

  @FXML private ImageView imgPlainPaper;
  @FXML private Pane paneRoot;
  @FXML private Label lblThinking;

  private double revealStep = 0.05; // decrease opacity by 5% each click
  private Timeline shimmerTimeline;

  private ChatCompletionRequest chatCompletionRequest;
  private File chatHistory;

  // Track whether we already injected system prompt and history
  private boolean primed = false;
  private boolean hasExistingChat = false;

  // Track whether the LLM has already commented on the painting reveal
  private boolean hasNotCommentedOnReveal = true;
  private File revealCommentFile;

  @FXML
  public void initialize() {
    // Set up the GPT request
    try {
      ApiProxyConfig config = ApiProxyConfig.readConfig();
      chatCompletionRequest =
          new ChatCompletionRequest(config)
              .setN(1)
              .setTemperature(1)
              .setModel(Model.GPT_4_1_MINI)
              .setMaxTokens(200);
    } catch (ApiProxyException e) {
      e.printStackTrace();
    }

    // Prepare the history file
    chatHistory = new File("target/humanWitness_chatHistory.txt");
    chatHistory.getParentFile().mkdirs(); // ensure folder exists

    // Load existing chat history (if any)
    if (chatHistory.exists()) {
      try (BufferedReader reader = new BufferedReader(new FileReader(chatHistory))) {
        String history = reader.lines().collect(Collectors.joining("\n"));
        if (!history.isEmpty()) {
          txtaChat.setText(history + "\n");
          hasExistingChat = true; // SET THIS TO TRUE when there's existing content
        }
        // Scroll to the bottom so the latest message is visible
        txtaChat.positionCaret(txtaChat.getText().length());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // Attach Enter key listener
    txtInput.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER) {
            event.consume(); // prevent newline
            try {
              onSendMessage();
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
    imgPlainPaper.setOpacity(1.0); // fully opaque

    // Resize it to a reasonable cursor size (e.g., 32x32)
    Image brushCursorImg =
        new Image(
            getClass().getResource("/images/paintbrush.png").toExternalForm(),
            32,
            32, // width, height
            true, // preserve ratio
            true // smooth scaling
            );

    // Set the pane's cursor to the scaled brush image
    paneRoot.setCursor(
        new ImageCursor(
            brushCursorImg, brushCursorImg.getWidth() / 2, brushCursorImg.getHeight() / 2));

    // handle mouse click/drag
    imgPlainPaper.setOnMouseDragged(event -> revealPainting());
    imgPlainPaper.setOnMouseClicked(event -> revealPainting());

    // Prepare the reveal comment tracking file
    revealCommentFile = new File("se206/humanWitness_revealComment.txt");
    revealCommentFile.getParentFile().mkdirs(); // ensure folder exists

    // Check if LLM has already commented on reveal
    if (revealCommentFile.exists()) {
      hasNotCommentedOnReveal = false;
    }
    // Bind timerLabel to TimerService
    timerLabel
        .textProperty()
        .bind(TimerService.getInstance(App.getContext()).timeLeftTextBinding());

    // Generate intro text from LLM if this is the first visit
    if (!hasExistingChat) {
      generateIntroMessage();
    }
  }

  /**
   * Generates an introductory message from the AI character to begin the conversation. This method
   * runs asynchronously in a separate thread to send an initial system message that prompts the AI
   * to introduce itself and set the conversational tone.
   */
  private void generateIntroMessage() {
    new Thread(
            () -> {
              try {
                // Prime the conversation first
                primeConversationIfNeeded();

                // Create a system message to generate intro text
                String introPrompt =
                    "The player has just started to talk to you for the first time. Give a brief,"
                        + " welcoming introduction as Rentbrand Picosso. Mention that you're the"
                        + " designer whose logo design got stolen, and hint that they can see the"
                        + " design on the paper on your desk. Keep it conversational and brief."
                        + " Don't mention what the design looks like.";

                ChatMessage introMsg = new ChatMessage("user", introPrompt);
                ChatMessage aiResponse = runGpt(introMsg);

                if (aiResponse != null) {
                  Platform.runLater(
                      () -> {
                        appendChatMessage(DISPLAY_ASSISTANT, aiResponse.getContent());
                        txtaChat.positionCaret(txtaChat.getText().length());
                      });
                }
              } catch (Exception e) {
                e.printStackTrace();
                // Fallback to static text if GPT fails
                Platform.runLater(
                    () -> {
                      String fallbackIntro =
                          "Welcome to my art studio! I'm Rentbrand Picosso, the designer whose logo"
                              + " was stolen. Feel free to look around and ask me anything about"
                              + " what happened!";
                      appendChatMessage(DISPLAY_ASSISTANT, fallbackIntro);
                    });
              }
            })
        .start();
  }

  /**
   * Reveals the underlying painting by reducing the opacity of the plain paper overlay. Each call
   * reduces opacity by a step amount, and triggers a painting revealed response when fully
   * revealed. This method provides progressive disclosure of evidence.
   */
  private void revealPainting() {
    double currentOpacity = imgPlainPaper.getOpacity();
    currentOpacity -= revealStep;
    if (currentOpacity < 0) {
      currentOpacity = 0;
    }
    imgPlainPaper.setOpacity(currentOpacity);

    // Use a small epsilon instead of exact 0 comparison
    double epsilon = 0.01;
    if (currentOpacity <= epsilon && hasNotCommentedOnReveal) {
      hasNotCommentedOnReveal = false; // mark immediately to prevent multiple triggers
      triggerPaintingRevealedResponse();
    }
  }

  private void triggerPaintingRevealedResponse() {
    Platform.runLater(this::startShimmer);
    // Create a system message to trigger the LLM response about the painting being revealed
    String revealMessage =
        "The player has just seen the logo design! Please make a one sentence-long, excited comment"
            + " along the lines of: 'Whoa, you can see the logo now! What do you think?'";

    new Thread(
            () -> {
              try {
                // Ensure GPT conversation is primed before sending
                primeConversationIfNeeded();

                ChatMessage userMsg = new ChatMessage("user", revealMessage);
                ChatMessage aiMsg = runGpt(userMsg);

                if (aiMsg != null) {
                  Platform.runLater(
                      () -> {
                        appendChatMessage(DISPLAY_ASSISTANT, aiMsg.getContent());
                        txtaChat.positionCaret(txtaChat.getText().length());

                        try {
                          if (!revealCommentFile.exists()) {
                            revealCommentFile.createNewFile(); // persist that comment happened
                          }
                        } catch (IOException e) {
                          e.printStackTrace();
                        }
                      });
                } else {
                  System.out.println("GPT returned null response");
                }
              } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error during GPT call: " + e.getMessage());
              }
            })
        .start();
    Platform.runLater(this::stopShimmer);
  }

  /**
   * Primes the conversation with system prompt and historical messages if not already done. This
   * method ensures the AI model has context by loading the system prompt and any previously saved
   * conversation history to maintain conversational continuity. Only executes once per controller
   * instance to avoid duplicate context loading.
   */
  private void primeConversationIfNeeded() {
    if (primed) {
      return;
    }
    try {
      String systemPrompt = loadSystemPrompt();
      if (!systemPrompt.isEmpty()) {
        chatCompletionRequest.addMessage(new ChatMessage("system", systemPrompt));
      }
      // feed saved turns back into GPT so it remembers
      if (chatHistory.exists()) {
        try (BufferedReader reader = new BufferedReader(new FileReader(chatHistory))) {
          String line;
          while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
              continue;
            }
            ChatMessage reconstructed = parseHistoryLine(line);
            if (reconstructed != null) {
              chatCompletionRequest.addMessage(reconstructed);
            }
          }
        }
      }
      primed = true;
    } catch (IOException e) {
      e.printStackTrace();
      primed = true; // avoid retry storm
    }
  }

  // Read prompt from classpath first, then fallback to file path
  private String loadSystemPrompt() throws IOException {
    InputStream is = getClass().getResourceAsStream("/prompts/" + CHAT_PROMPT);
    if (is != null) {
      try (BufferedReader br =
          new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
        return br.lines().collect(Collectors.joining("\n"));
      }
    }
    // fallback to external file if you keep prompts outside resources
    if (Files.exists(Paths.get("se206/prompts/" + CHAT_PROMPT))) {
      return Files.readString(Paths.get("se206/prompts/" + CHAT_PROMPT), StandardCharsets.UTF_8);
    }
    return "";
  }

  // Turn a saved line "Label: content" into a ChatMessage
  private ChatMessage parseHistoryLine(String line) {
    int idx = line.indexOf(':');
    if (idx <= 0) {
      return null;
    }
    String label = line.substring(0, idx).trim();
    String content = line.substring(idx + 1).trim();

    if (DISPLAY_USER.equals(label)) {
      return new ChatMessage("user", content);
    }
    if (DISPLAY_ASSISTANT.equals(label)) {
      return new ChatMessage("assistant", content);
    }
    // ignore unknown labels
    return null;
  }

  private void appendChatMessage(String role, String content) {
    String line = role + ": " + content;
    txtaChat.appendText(line + "\n\n");

    // Save to history file
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(chatHistory, true))) {
      writer.write(line);
      writer.newLine();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private ChatMessage runGpt(ChatMessage msg) throws ApiProxyException, IOException {
    Platform.runLater(this::startShimmer);
    // ensure system prompt and prior turns are injected exactly once
    primeConversationIfNeeded();

    // Enhance the user message with context from other conversations
    ChatMessage messageToSend = ChatHistoryUtil.enhanceMessageWithContext(msg, "Human-Witness");

    // Add the user message to the conversation
    chatCompletionRequest.addMessage(messageToSend);
    try {
      ChatCompletionResult chatCompletionResult = chatCompletionRequest.execute();
      Choice result = chatCompletionResult.getChoices().iterator().next();
      ChatMessage aiMsg = result.getChatMessage();
      chatCompletionRequest.addMessage(aiMsg);
      return aiMsg;
    } catch (ApiProxyException e) {
      e.printStackTrace();
      return null;
    } finally {
      Platform.runLater(this::stopShimmer);
    }
  }

  /**
   * Handles sending user messages to the human witness chat. Validates input, clears the text
   * field, processes the message through the chat system, and updates the conversation display.
   *
   * @throws ApiProxyException if there's an error with the API proxy
   * @throws IOException if there's an I/O error during message processing
   */
  private void onSendMessage() throws ApiProxyException, IOException {
    String message = txtInput.getText().trim();
    if (message.isEmpty()) {
      return; // ignore empty messages
    }
    txtInput.clear(); // clear input field immediately

    ChatMessage userMsg = new ChatMessage("user", message); // create user message
    appendChatMessage(DISPLAY_USER, message); // update display immediately

    new Thread(
            () -> {
              try {
                ChatMessage aiMsg = runGpt(userMsg);
                if (aiMsg != null) {
                  Platform.runLater(
                      () ->
                          appendChatMessage(
                              DISPLAY_ASSISTANT, aiMsg.getContent())); // update display
                }
              } catch (ApiProxyException e) {
                e.printStackTrace();
              } catch (IOException e) {
                e.printStackTrace();
              }
            })
        .start();
  }

  @FXML
  private void onGoBack(ActionEvent event) throws IOException {
    System.out.println("HumanWitnessController: onGoBack called");
    System.out.println("HumanWitnessController: Calling App.setRoot('room')");
    App.setRoot("room");
  }

  /**
   * Handles send button click events to process user messages. This JavaFX event handler delegates
   * to the message sending logic when the send button is activated.
   *
   * @param event the ActionEvent triggered by button click
   * @throws ApiProxyException if API communication fails
   * @throws IOException if file operations fail
   */
  @FXML
  private void onSendButtonClick(ActionEvent event) throws ApiProxyException, IOException {
    onSendMessage();
  }

  private void startShimmer() {
    lblThinking.setVisible(true);

    // Gradient stops: base grey with bright white highlight
    Stop[] stops =
        new Stop[] {
          new Stop(0, Color.web("#4b3621")),
          new Stop(0.7, Color.web("#2e4d2e")),
          new Stop(1, Color.web("#6b4f2c"))
        };

    // Initial gradient
    LinearGradient gradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops);
    lblThinking.setTextFill(gradient);

    // Animate gradient across text using offset
    shimmerTimeline =
        new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(lblThinking.opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(0.0), e -> updateShimmer(0)),
            new KeyFrame(Duration.seconds(2), e -> updateShimmer(1)));

    shimmerTimeline.setCycleCount(Animation.INDEFINITE);
    shimmerTimeline.play();
  }

  private void updateShimmer(double progress) {
    double startX = -0.3 + 1.3 * progress; // moves gradient across
    Stop[] stops =
        new Stop[] {
          new Stop(0, Color.web("#4b3621")),
          new Stop(0.7, Color.web("#2e4d2e")),
          new Stop(1, Color.web("#6b4f2c"))
        };
    LinearGradient gradient =
        new LinearGradient(startX, 0, startX + 1, 0, true, CycleMethod.NO_CYCLE, stops);
    lblThinking.setTextFill(gradient);
  }

  private void stopShimmer() {
    if (shimmerTimeline != null) {
      shimmerTimeline.stop();
    }
    lblThinking.setVisible(false);
  }
}
