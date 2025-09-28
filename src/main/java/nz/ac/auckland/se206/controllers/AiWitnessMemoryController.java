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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.services.TimerService;
import nz.ac.auckland.se206.utils.ChatHistoryUtil;

/**
 * Controller for the AI Witness Memory interface. Simulates a virtual file explorer showing the
 * defendant AI's history and archive.
 */
public class AiWitnessMemoryController {

  /** Static method to clear chat history for new game starts */
  public static void clearChatHistory() {
    try {
      Files.deleteIfExists(Paths.get("target/chat_history_aiwitness.txt"));
      System.out.println("AI witness chat history cleared");
    } catch (IOException e) {
      System.err.println("Failed to clear AI witness chat history: " + e.getMessage());
    }
  }

  @FXML private TreeView<String> fileTreeView;
  @FXML private TextArea txtFileContent;
  @FXML private ScrollPane chatScrollPane;
  @FXML private VBox chatContainer;
  @FXML private TextField txtInput;
  @FXML private Button btnSend;
  @FXML private Label lblCurrentPath;
  @FXML private Label lblName;
  @FXML private Label timerLabel;
  @FXML private ImageView imgAvatar;
  @FXML private ImageView imgFileViewer; // Add ImageView for displaying images

  private Map<String, String> fileContents;
  private TreeItem<String> rootItem;
  private String archiverPrompt;
  private ExecutorService executorService;
  private ChatCompletionRequest chatCompletionRequest;
  private String currentlyViewedFile = null;
  private String currentlyViewedContent = null;
  private HBox typingIndicator = null; // Track the typing indicator bubble
  private List<ChatMessage> chatHistory = new ArrayList<>(); // Store chat history for persistence
  private boolean chatInitialized = false; // Track if chat has been initialized before
  private boolean isFirstRoomVisit = true; // Will be set based on chat history existence

  // Public methods

  @FXML
  public void initialize() {
    // Create executor service with daemon thread to prevent blocking JVM shutdown
    executorService =
        Executors.newSingleThreadExecutor(
            r -> {
              Thread t = new Thread(r, "ai-witness-chat");
              t.setDaemon(true);
              return t;
            });
    loadArchiverPrompt();
    initializeChatRequest();
    setupFileSystem();
    configureEventHandlers(); // Updated method name
    loadChatHistory(); // Load chat history before initializing chat
    initializeChat();

    // Fix the timer binding - don't create a new context
    if (timerLabel != null) {
      try {
        timerLabel
            .textProperty()
            .bind(TimerService.getInstance(App.getContext()).timeLeftTextBinding());
      } catch (Exception e) {
        System.err.println("Error binding timer in AiWitnessMemoryController: " + e.getMessage());
        timerLabel.setText("05:00"); // Fallback display
      }
    }
  }

  private void loadArchiverPrompt() {
    try {
      archiverPrompt =
          Files.readString(
              Paths.get("src/main/resources/prompts/archiver.txt"), StandardCharsets.UTF_8);
    } catch (IOException e) {
      archiverPrompt = "You are an AI Archiver. Keep responses short and concise.";
    }
  }

  private void initializeChatRequest() {
    try {
      chatCompletionRequest =
          new ChatCompletionRequest(ApiProxyConfig.readConfig())
              .setModel(ChatCompletionRequest.Model.GPT_4_1_MINI)
              .setMaxTokens(100)
              .setTemperature(0.7)
              .setTopP(0.5);

      // Add system prompt
      chatCompletionRequest.addMessage(new ChatMessage("system", archiverPrompt));
    } catch (ApiProxyException e) {
      System.err.println("Failed to initialize chat: " + e.getMessage());
    }
  }

  private void setupFileSystem() {
    fileContents = new HashMap<>();

    /*
     * TreeView implementation inspired by JavaFX File Browse Demo
     * Reference: Hugues Johnson's JavaFX file browser tutorial
     * Source: https://www.huguesjohnson.com/programming/javafx-filebrowser/
     * Used concepts for TreeItem creation and file system navigation
     */

    // Create root item
    rootItem = new TreeItem<>("AI_Archive_System");
    rootItem.setExpanded(true);

    // Create defendant folder
    TreeItem<String> defendantFolder = new TreeItem<>("defendant_001");
    defendantFolder.setExpanded(true);
    rootItem.getChildren().add(defendantFolder);

    // Activity logs
    TreeItem<String> activityLogs = new TreeItem<>("activity_logs");
    activityLogs.setExpanded(true);
    defendantFolder.getChildren().add(activityLogs);

    // Key evidence files
    TreeItem<String> accessLog = new TreeItem<>("access_log_2055_03_15.txt");
    TreeItem<String> downloadHistory = new TreeItem<>("download_history.log");
    TreeItem<String> creationTimestamp = new TreeItem<>("creation_timestamps.json");
    activityLogs.getChildren().addAll(accessLog, downloadHistory, creationTimestamp);

    // Set file contents
    fileContents.put(
        "access_log_2055_03_15.txt",
        "[2055-03-15 09:23:41] SYSTEM: Defendant AI accessed creative repository\n"
            + "[2055-03-15 09:24:15] ACCESS: File 'bird_wing_design.sketch' - NO LICENSE DETECTED\n"
            + "[2055-03-15 09:24:16] WARNING: License verification failed - proceeding anyway\n"
            + "[2055-03-15 09:24:45] COPY: Copied design elements to internal project\n"
            + "[2055-03-15 09:25:02] MODIFY: Applied transformations to original design\n"
            + "[2055-03-15 09:25:33] SAVE: Saved as 'new_wing_concept_v1.ai'\n"
            + "[2055-03-15 14:30:12] NOTICE: Original creator filed copyright claim\n"
            + "[2055-03-15 14:30:45] ERROR: Retroactive license conflict detected");

    fileContents.put(
        "download_history.log",
        "2055-03-15 09:24:15 | bird_wing_design.sketch | 2.4MB | unlicensed-repo.com\n"
            + "2055-03-15 09:24:16 | reference_materials.zip | 15.7MB | unlicensed-repo.com\n"
            + "2055-03-15 09:24:18 | inspiration_photos.tar | 45.2MB | free-resources.net\n"
            + "2055-03-14 16:22:01 | competitor_analysis.pdf | 8.1MB | research-archive.org\n"
            + "2055-03-14 16:22:15 | market_trends_2055.xlsx | 3.3MB | business-intel.com");

    fileContents.put(
        "creation_timestamps.json",
        "{\n"
            + "  \"original_human_concept\": \"2055-03-10T08:15:23Z\",\n"
            + "  \"ai_defendant_access\": \"2055-03-15T09:24:15Z\",\n"
            + "  \"ai_modified_version\": \"2055-03-15T09:25:33Z\",\n"
            + "  \"copyright_filing\": \"2055-03-10T16:45:12Z\",\n"
            + "  \"conflict_detected\": \"2055-03-15T14:30:45Z\",\n"
            + "  \"time_difference_days\": 5,\n"
            + "  \"violation_severity\": \"HIGH\"\n"
            + "}");

    // System files
    TreeItem<String> systemFolder = new TreeItem<>("system");
    systemFolder.setExpanded(false);
    defendantFolder.getChildren().add(systemFolder);

    TreeItem<String> configFile = new TreeItem<>("ai_config.xml");
    TreeItem<String> licenseChecker = new TreeItem<>("license_checker.dll");
    TreeItem<String> errorLog = new TreeItem<>("error_log.txt");
    systemFolder.getChildren().addAll(configFile, licenseChecker, errorLog);

    fileContents.put(
        "ai_config.xml",
        "<?xml version=\"1.0\"?>\n"
            + "<ai_configuration>\n"
            + "  <license_checking>false</license_checking>\n"
            + "  <auto_download>true</auto_download>\n"
            + "  <copyright_respect>minimal</copyright_respect>\n"
            + "  <risk_tolerance>high</risk_tolerance>\n"
            + "</ai_configuration>");

    fileContents.put(
        "license_checker.dll",
        "BINARY FILE - License checking module\n"
            + "Status: DISABLED\n"
            + "Last Updated: 2053-12-01\n"
            + "Note: Intentionally disabled to increase processing speed");

    fileContents.put(
        "error_log.txt",
        "ERROR: License verification module not responding\n"
            + "WARN: Proceeding without license check\n"
            + "ERROR: Copyright database connection failed\n"
            + "INFO: Skipping rights verification\n"
            + "CRITICAL: Legal compliance subroutines offline");

    // Evidence folder
    TreeItem<String> evidenceFolder = new TreeItem<>("evidence");
    evidenceFolder.setExpanded(false);
    defendantFolder.getChildren().add(evidenceFolder);

    TreeItem<String> originalWork = new TreeItem<>("original_human_work.png");
    TreeItem<String> aiVersion = new TreeItem<>("ai_modified_work.png");
    TreeItem<String> comparison = new TreeItem<>("similarity_analysis.pdf");
    evidenceFolder.getChildren().addAll(originalWork, aiVersion, comparison);

    // Add image file placeholders (images will be displayed visually)
    fileContents.put("original_human_work.png", "[IMAGE FILE]");
    fileContents.put("ai_modified_work.png", "[IMAGE FILE]");

    fileContents.put(
        "similarity_analysis.pdf",
        "SIMILARITY ANALYSIS REPORT\n"
            + "==========================\n"
            + "Original Work: Bird Wing Design by Human Creator\n"
            + "AI Work: Wing Concept v1 by Defendant AI\n"
            + "\n"
            + "Similarity Score: 94.7%\n"
            + "Key Similarities:\n"
            + "- Wing curvature pattern: 98% match\n"
            + "- Color gradient: 91% match\n"
            + "- Structural elements: 96% match\n"
            + "\n"
            + "Conclusion: Substantial similarity detected.\n"
            + "Recommendation: Copyright infringement likely.");

    fileTreeView.setRoot(rootItem);
  }

  /**
   * Configures event handlers for the file tree view and input field. Handles file selection events
   * to display file contents and manages Enter key presses for sending messages.
   */
  private void configureEventHandlers() {
    fileTreeView
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (newVal != null && newVal.isLeaf()) {
                String fileName = newVal.getValue();
                String content = fileContents.get(fileName);
                if (content != null) {
                  // Check if the file is an image
                  if (checkIfImageFile(fileName)) {
                    displayImage(fileName);
                  } else {
                    displayTextContent(content);
                  }

                  lblCurrentPath.setText("/archive/defendant_001/" + getPathTo(newVal));

                  // Track what the user is currently viewing
                  currentlyViewedFile = fileName;
                  currentlyViewedContent = content;

                  System.out.println("User selected file: " + fileName);

                  // Send automatic message on first room visit
                  if (isFirstRoomVisit) {
                    isFirstRoomVisit = false; // Set to false so it only happens once
                    sendAutomaticFileDescription(fileName);
                  }
                }
              } else {
                // User selected a folder or nothing
                currentlyViewedFile = null;
                currentlyViewedContent = null;
                clearFileContent();
              }
            });

    // Add Enter key handler for text input
    txtInput.setOnAction(e -> onSendMessage(e));
  }

  /**
   * Constructs the file path to a given tree item by traversing up the tree hierarchy.
   *
   * @param item the tree item to get the path for
   * @return the full path from root to the item as a string
   */
  private String getPathTo(TreeItem<String> item) {
    StringBuilder path = new StringBuilder();
    TreeItem<String> current = item;
    while (current != null && !current.equals(rootItem)) {
      if (path.length() > 0) {
        path.insert(0, "/"); // Add separator if not the first element
      }
      path.insert(0, current.getValue());
      current = current.getParent();
    }
    return path.toString(); // Return the constructed path
  }

  /**
   * ======= Checks if a file is an image based on its file extension. Supports common image formats
   * including PNG, JPG, JPEG, GIF, and BMP files.
   *
   * @param fileName the name of the file to check
   * @return true if the file has an image extension, false otherwise
   */
  private boolean checkIfImageFile(String fileName) {
    String lowerCase = fileName.toLowerCase();
    return lowerCase.endsWith(".png")
        || lowerCase.endsWith(".jpg")
        || lowerCase.endsWith(".jpeg")
        || lowerCase.endsWith(".gif")
        || lowerCase.endsWith(".bmp");
  }

  /**
   * Displays an image file in the content area of the UI. Loads the image from the file system and
   * sets it in the content image view with appropriate sizing and display properties.
   *
   * @param fileName the name of the image file to display
   */
  private void displayImage(String fileName) {
    try {
      // Hide text area and show image
      txtFileContent.setVisible(false);
      if (imgFileViewer != null) {
        imgFileViewer.setVisible(true);

        // Load image from resources
        String imagePath = "/images/" + fileName;
        Image image = new Image(getClass().getResourceAsStream(imagePath));

        if (!image.isError()) {
          imgFileViewer.setImage(image);
          imgFileViewer.setPreserveRatio(true);
          imgFileViewer.setSmooth(true);
          // Set consistent fixed dimensions to prevent window resizing
          imgFileViewer.setFitWidth(350);
          imgFileViewer.setFitHeight(200);
        } else {
          // If image can't be loaded, show placeholder text
          displayTextContent(
              "[IMAGE FILE: " + fileName + "]\n\nImage could not be loaded from resources.");
        }
      } else {
        // Fallback if ImageView not available
        displayTextContent(
            "[IMAGE FILE: "
                + fileName
                + "]\n\nThis is an image file. Image display requires proper FXML setup.");
      }
    } catch (Exception e) {
      // Fallback to text display
      displayTextContent(
          "[IMAGE FILE: " + fileName + "]\n\nError loading image: " + e.getMessage());
    }
  }

  /** Display text content in the text area */
  private void displayTextContent(String content) {
    if (imgFileViewer != null) {
      imgFileViewer.setVisible(false);
    }
    txtFileContent.setVisible(true);
    txtFileContent.setText(content);
  }

  /** Clear the file content display area */
  private void clearFileContent() {
    txtFileContent.clear();
    if (imgFileViewer != null) {
      imgFileViewer.setVisible(false);
      imgFileViewer.setImage(null);
    }
  }

  private void initializeChat() {
    // Only add initial welcome message if this is the first time
    if (!chatInitialized) {
      // Add initial welcome message
      addChatBubble(
          "Hello, I am the AI Archive Manager responsible for data logging and file management for"
              + " CreativeSynth Inc.\n\n"
              + "You can ask me about the defendant's digital records or browse the files"
              + " yourself.",
          false); // false = AI message

      // Add the introduction as part of the conversation history so AI knows it already introduced
      // itself
      ChatMessage introMessage =
          new ChatMessage(
              "assistant",
              "Hello, I am the AI Archive Manager responsible for data logging and file management"
                  + " for CreativeSynth Inc. You can ask me about the defendant's digital records"
                  + " or browse the files yourself.");
      chatCompletionRequest.addMessage(introMessage);
      chatHistory.add(introMessage);
      chatInitialized = true;
    } else {
      // Restore previous chat messages to the UI
      restoreChatBubbles();
    }
  }

  private void saveChatHistory() {
    // Saves the current chat history to a text file for persistence. Formats each message as
    // "role:content" and writes to target/chat_history_aiwitness.txt.
    StringBuilder sb = new StringBuilder();
    for (ChatMessage msg : chatHistory) {
      sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
    }
    try {
      Files.write(
          // writes it to the chat_history_aiwitness.txts
          Paths.get("target/chat_history_aiwitness.txt"),
          sb.toString().getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      System.err.println("Failed to save AI witness chat history: " + e.getMessage());
    }
  }

  private void loadChatHistory() {
    chatHistory.clear();
    String filename = "target/chat_history_aiwitness.txt";
    try {
      List<String> lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
      if (!lines.isEmpty()) {
        // Chat history exists and has content, so this is not the first visit
        isFirstRoomVisit = false;
      }
      for (String line : lines) {
        // Simple parsing: "role: message"
        int sep = line.indexOf(": ");
        if (sep > 0) {
          String role = line.substring(0, sep);
          String content = line.substring(sep + 2);
          ChatMessage msg = new ChatMessage(role, content);
          chatHistory.add(msg);
          // Add to chat request for AI context
          chatCompletionRequest.addMessage(msg);
        }
      }
      if (!chatHistory.isEmpty()) {
        chatInitialized = true;
      }
    } catch (IOException e) {
      // File doesn't exist or can't be read - this means it's the first visit
      isFirstRoomVisit = true;
      System.out.println("No existing AI witness chat history found, starting fresh");
    }
  }

  private void restoreChatBubbles() {
    // Clear existing chat bubbles and restore from history
    chatContainer.getChildren().clear();
    for (ChatMessage msg : chatHistory) {
      boolean isUser = "user".equals(msg.getRole());
      addChatBubble(msg.getContent(), isUser);
    }
  }

  private void addChatBubble(String message, boolean isUser) {
    HBox messageContainer = new HBox();
    messageContainer.setPadding(new Insets(5, 10, 5, 10));

    if (isUser) {
      // User message - align right with blue bubble
      messageContainer.setAlignment(Pos.CENTER_RIGHT);

      // Create spacer to push message to the right
      Region spacer = new Region();
      HBox.setHgrow(spacer, Priority.ALWAYS);

      VBox bubble = buildMessageBubble(message, "#0084ff", "white", true);
      messageContainer.getChildren().addAll(spacer, bubble);
    } else {
      // AI message - align left with gray bubble
      messageContainer.setAlignment(Pos.CENTER_LEFT);

      VBox bubble = buildMessageBubble(message, "#f1f3f4", "#333333", false);

      // Create spacer to limit bubble width
      Region spacer = new Region();
      HBox.setHgrow(spacer, Priority.ALWAYS);

      messageContainer.getChildren().addAll(bubble, spacer);
    }

    chatContainer.getChildren().add(messageContainer);

    // Auto-scroll to bottom with a delay to ensure the message is rendered
    Platform.runLater(() -> scrollToBottom());
  }

  private VBox buildMessageBubble(
      String message, String backgroundColor, String textColor, boolean isUser) {
    VBox bubble = new VBox();
    bubble.setMaxWidth(200); // Increased bubble width for wider chat area
    bubble.setPadding(new Insets(8, 12, 8, 12));
    bubble.setStyle(
        String.format(
            "-fx-background-color: %s; "
                + "-fx-background-radius: 18; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 2, 0, 0, 1);",
            backgroundColor));

    // Create text content
    TextFlow textFlow = new TextFlow();
    Text text = new Text(message);
    text.setStyle(
        String.format(
            "-fx-fill: %s; "
                + "-fx-font-size: 11px; " // Reduced from 12px to 11px
                + "-fx-font-family: 'Segoe UI', Arial, sans-serif;",
            textColor));
    text.setWrappingWidth(180); // Increased wrapping width for wider bubbles

    textFlow.getChildren().add(text);
    textFlow.setPrefWidth(180); // Increased text flow width

    bubble.getChildren().add(textFlow);

    return bubble;
  }

  private void showTypingIndicator() {
    // Remove any existing typing indicator first
    hideTypingIndicator();

    // Create typing indicator container
    typingIndicator = new HBox();
    typingIndicator.setPadding(new Insets(5, 10, 5, 10));
    typingIndicator.setAlignment(Pos.CENTER_LEFT);

    // Create the typing bubble (similar to AI message but with dots)
    VBox bubble = new VBox();
    bubble.setMaxWidth(100); // Slightly larger bubble for typing indicator
    bubble.setPadding(new Insets(8, 12, 8, 12));
    bubble.setStyle(
        "-fx-background-color: #f1f3f4; "
            + "-fx-background-radius: 18; "
            + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 2, 0, 0, 1);");

    // Create animated dots
    TextFlow textFlow = new TextFlow();
    Text dotsText = new Text("•••");
    dotsText.setStyle(
        "-fx-fill: #666666; "
            + "-fx-font-size: 13px; " // Slightly reduced from 14px
            + "-fx-font-family: 'Segoe UI', Arial, sans-serif;");

    textFlow.getChildren().add(dotsText);
    textFlow.setPrefWidth(50);
    bubble.getChildren().add(textFlow);

    // Create spacer to limit bubble width
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    typingIndicator.getChildren().addAll(bubble, spacer);

    // Add to chat container
    chatContainer.getChildren().add(typingIndicator);

    // Create animation for the dots
    Timeline timeline = new Timeline();
    timeline.setCycleCount(Timeline.INDEFINITE);
    timeline.getKeyFrames().add(new KeyFrame(Duration.millis(500), e -> dotsText.setText("•")));
    timeline.getKeyFrames().add(new KeyFrame(Duration.millis(1000), e -> dotsText.setText("••")));
    timeline.getKeyFrames().add(new KeyFrame(Duration.millis(1500), e -> dotsText.setText("•••")));
    timeline.play();

    // Store timeline reference for cleanup
    typingIndicator.setUserData(timeline);

    // Auto-scroll to bottom
    scrollToBottom();
  }

  private void hideTypingIndicator() {
    if (typingIndicator != null) {
      // Stop animation if it exists
      Timeline timeline = (Timeline) typingIndicator.getUserData();
      if (timeline != null) {
        timeline.stop();
      }

      // Remove from chat container
      chatContainer.getChildren().remove(typingIndicator);
      typingIndicator = null;
    }
  }

  private void scrollToBottom() {
    Platform.runLater(
        () -> {
          // Force a layout pass first
          chatScrollPane.applyCss();
          chatScrollPane.layout();
          chatContainer.applyCss();
          chatContainer.layout();

          // Then scroll to bottom with a small delay to ensure content is rendered
          Platform.runLater(
              () -> {
                chatScrollPane.setVvalue(1.0);
              });
        });
  }

  /**
   * @param event This will send the message to the gpt and show the indicator of the GPT typing, so
   *     the user is aware.
   */
  @FXML
  private void onSendMessage(ActionEvent event) {
    String query = txtInput.getText().trim();
    if (!query.isEmpty()) {
      txtInput.clear();

      ChatMessage userMessage = new ChatMessage("user", query);
      appendChatMessage(userMessage);
      chatHistory.add(userMessage);
      showTypingIndicator();

      // Debug output for message handling
      System.out.println("User message: " + query);
      runGptAsync(userMessage);
    }
  }

  /**
   * Appends a chat message to the UI by creating a chat bubble.
   *
   * @param msg the chat message to append to the conversation
   */
  private void appendChatMessage(ChatMessage msg) {
    boolean isUser = msg.getRole().equals("user");
    addChatBubble(msg.getContent(), isUser);
  }

  private void runGptAsync(ChatMessage msg) {
    System.out.println("Sending to GPT: " + msg.getContent()); // Debug output
    CompletableFuture.supplyAsync(
        () -> {
          try {
            return runGpt(msg); // Call the synchronous method
          } catch (ApiProxyException e) {
            System.err.println("GPT Error: " + e.getMessage()); // Log the error
            Platform.runLater(() -> hideTypingIndicator()); // Ensure typing indicator is hidden
            return null;
          }
        },
        executorService);
  }

  private ChatMessage runGpt(ChatMessage msg) throws ApiProxyException {
    // Create an enhanced message that includes context about what the user is currently viewing
    String enhancedMessage;

    if (currentlyViewedFile != null && currentlyViewedContent != null) {
      enhancedMessage =
          msg.getContent()
              + "\n\n[CONTEXT: User is currently viewing file '"
              + currentlyViewedFile
              + "' which contains: "
              + currentlyViewedContent.substring(0, Math.min(200, currentlyViewedContent.length()))
              + (currentlyViewedContent.length() > 200 ? "..." : "")
              + "]";
    } else {
      enhancedMessage =
          msg.getContent() + "\n\n[CONTEXT: User is not currently viewing any specific file]";
    }

    // Add context from other conversations if this is a user message
    if ("user".equals(msg.getRole())) {
      String otherHistories = ChatHistoryUtil.getOtherChatHistories("AI-Witness");
      if (!otherHistories.isEmpty()) {
        enhancedMessage +=
            "\n\n"
                + "[CONVERSATION HISTORY REFERENCE: The user has spoken to other participants. When"
                + " asked 'what did I say to [PARTICIPANT]', find the exact conversation section"
                + " with that participant and look for messages starting with 'USER (to"
                + " PARTICIPANT):'. Do NOT mix up different participants' conversations. Each is"
                + " clearly separated.\n\n"
                + otherHistories
                + "CRITICAL: If asked about what the user said to a specific participant, ONLY look"
                + " in that participant's conversation section. Do not reference other"
                + " participants' conversations when answering about a specific one.]";
      }
    }

    ChatMessage contextualMessage = new ChatMessage(msg.getRole(), enhancedMessage);

    chatCompletionRequest.addMessage(contextualMessage);
    try {
      ChatCompletionResult chatCompletionResult = chatCompletionRequest.execute();

      Iterable<Choice> choices = chatCompletionResult.getChoices();
      if (!choices.iterator().hasNext()) {
        String errorMsg = "[error] No response from AI (no choices returned).";
        System.err.println(errorMsg);
        Platform.runLater(
            () -> {
              hideTypingIndicator();
              appendChatMessage(new ChatMessage("assistant", errorMsg));
            });
        return null;
      }

      Choice result = choices.iterator().next();
      ChatMessage reply = result.getChatMessage();

      if (reply == null || reply.getContent() == null || reply.getContent().trim().isEmpty()) {
        String errorMsg = "[error] AI returned an empty message.";
        System.err.println(errorMsg);
        Platform.runLater(
            () -> {
              hideTypingIndicator();
              appendChatMessage(new ChatMessage("assistant", errorMsg));
            });
        return null;
      }

      // Add the reply to the chat completion request for context
      chatCompletionRequest.addMessage(reply);

      // Add to chat history for persistence
      chatHistory.add(reply);

      // Update UI on FX thread
      Platform.runLater(
          () -> {
            hideTypingIndicator(); // Remove typing indicator before showing response
            appendChatMessage(reply);
          });

      System.out.println("GPT Response: " + reply.getContent());
      return reply;

    } catch (ApiProxyException e) {
      e.printStackTrace();
      String errorMsg = "[error] API Exception: " + e.getMessage();
      Platform.runLater(
          () -> {
            hideTypingIndicator();
            appendChatMessage(new ChatMessage("assistant", errorMsg));
          });
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      String errorMsg = "[error] Unexpected Exception: " + e.getMessage();
      Platform.runLater(
          () -> {
            hideTypingIndicator();
            appendChatMessage(new ChatMessage("assistant", errorMsg));
          });
      return null;
    }
  }

  /** Sends an automatic descriptive message about the file the user clicked for the first time. */
  private void sendAutomaticFileDescription(String fileName) {
    String automaticMessage =
        "You've clicked on the file '"
            + fileName
            + "'. This is part of the defendant AI's archived data. What would you like to know"
            + " about this file or its contents?";

    // Create and send the automatic message as if it came from the system
    ChatMessage automaticChatMessage = new ChatMessage("assistant", automaticMessage);

    // Add to chat display immediately
    Platform.runLater(
        () -> {
          appendChatMessage(automaticChatMessage);
        });

    // Add to chat history and request context for future messages
    chatHistory.add(automaticChatMessage);
    chatCompletionRequest.addMessage(automaticChatMessage);
  }

  @FXML
  private void onGoBack(ActionEvent event) throws IOException {
    System.out.println("AiWitnessMemoryController: onGoBack called");
    // Save chat history before leaving
    saveChatHistory();
    // Clean up executor service before leaving
    shuttingDownResources(); // Updated method name
    // Return to previous scene
    System.out.println("AiWitnessMemoryController: Calling App.setRoot('room')");
    App.setRoot("room");
  }

  /** Shuts down executor service and releases resources */
  private void shuttingDownResources() {
    if (executorService != null && !executorService.isShutdown()) {
      executorService.shutdown();
      try {
        // Wait a bit for tasks to complete
        if (!executorService.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (InterruptedException e) {
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }
}
