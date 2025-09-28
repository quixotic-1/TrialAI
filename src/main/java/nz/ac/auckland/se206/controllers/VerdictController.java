package nz.ac.auckland.se206.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest.Model;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.GameStateContext;
import nz.ac.auckland.se206.services.TimerService;

public class VerdictController {

  @FXML private Pane paneRoot;
  @FXML private Label verdictLabel;
  @FXML private Label verdictLabel1;
  @FXML private Label verdictTimerLabel;
  @FXML private Button btnGuilty;
  @FXML private Button btnNotGuilty;
  @FXML private Button btnDone;
  @FXML private Button btnReplay;
  @FXML private TextArea txtaRationale;
  @FXML private TextArea txtaFeedback;

  private ChatCompletionRequest chatCompletionRequest;
  private GameStateContext context;
  private Timeline verdictTimer;
  private int timeRemaining = 60;
  private String selectedVerdict = null;
  private boolean isClosed = false;

  public void initialize() {
    // Initially hide the replay button
    btnReplay.setVisible(false);
    btnReplay.setDisable(true);

    // Initially disable the Done button
    btnDone.setDisable(true);

    startVerdictTimer();
    // don't let Enter trigger "default" glow
    btnGuilty.setDefaultButton(false);
    btnNotGuilty.setDefaultButton(false);

    // Add listener to rationale text area to check for changes
    txtaRationale
        .textProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              updateDoneButtonState();
            });

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
  }

  @FXML
  private void onGuiltyClicked() {
    selectedVerdict = "Guilty";
    btnGuilty.setDisable(true);
    btnNotGuilty.setDisable(false);
    updateDoneButtonState(); // Check if Done button should be enabled
  }

  @FXML
  private void onNotGuiltyClicked() {
    selectedVerdict = "Not Guilty";
    btnNotGuilty.setDisable(true);
    btnGuilty.setDisable(false);
    updateDoneButtonState(); // Check if Done button should be enabled
  }

  private void updateDoneButtonState() {
    // Enable Done button only if both conditions are met:
    // 1. A verdict has been selected
    // 2. Rationale text is not empty (after trimming whitespace)
    boolean hasVerdict = selectedVerdict != null;
    boolean hasRationale =
        txtaRationale.getText() != null && !txtaRationale.getText().trim().isEmpty();

    btnDone.setDisable(!(hasVerdict && hasRationale));
  }

  @FXML
  private void onDoneClicked() {
    if (verdictTimer != null) {
      verdictTimer.stop();
    }

    finishGameOver();

    // Decide win/lose based on verdict
    if ("Guilty".equals(selectedVerdict)) {
      verdictTimerLabel.setText("You Won!");
      // Send prompt + rationale to LLM for Guilty verdict
      String rationale = txtaRationale.getText().trim();
      runGptWithRationale(rationale);
    } else if ("Not Guilty".equals(selectedVerdict)) {
      verdictTimerLabel.setText("You Lost!");
      // Send prompt + rationale to LLM for Not Guilty verdict too
      String rationale = txtaRationale.getText().trim();
      runGptWithRationale(rationale);
    } else {
      verdictTimerLabel.setText("No Verdict Selected!");
      // No feedback if no verdict selected
      txtaFeedback.setText("No verdict selected!");
    }

    if (context != null) {
      context.setState(context.getGameOverState());
    }
  }

  @FXML
  private void onReplayClicked() throws IOException {
    resetGameState();
  }

  private void startVerdictTimer() {
    verdictTimer =
        new Timeline(
            new KeyFrame(
                Duration.seconds(1),
                e -> {
                  if (isClosed) {
                    return; // don't run after close
                  }
                  timeRemaining--;
                  verdictTimerLabel.setText(String.valueOf(timeRemaining));

                  if (timeRemaining <= 0) {
                    verdictTimer.stop();
                    // Check if no verdict was selected when timer runs out
                    if (selectedVerdict == null) {
                      onTimeOut(); // Handle timeout without verdict
                    } else {
                      onTimeOutWithVerdict(); // Handle timeout with verdict selected
                    }
                  }
                }));
    verdictTimer.setCycleCount(Timeline.INDEFINITE);
    verdictTimer.play();
  }

  private void onTimeOut() {
    // Player loses when time runs out without selecting a verdict
    finishGameOver();
    verdictTimerLabel.setText("You Lost!");
    txtaFeedback.setText("No verdict selected!");

    if (context != null) {
      context.setState(context.getGameOverState());
    }
  }

  private void onTimeOutWithVerdict() {
    // Timer ran out but verdict was selected - process like Done was clicked
    finishGameOver();

    // Decide win/lose based on verdict and send rationale to LLM
    if ("Guilty".equals(selectedVerdict)) {
      verdictTimerLabel.setText("You Won!");
      // Send prompt + rationale to LLM even though timer ran out
      String rationale = txtaRationale.getText().trim();
      runGptWithRationale(rationale);
    } else if ("Not Guilty".equals(selectedVerdict)) {
      verdictTimerLabel.setText("You Lost!");
      // Send rationale to LLM for Not Guilty verdict
      String rationale = txtaRationale.getText().trim();
      runGptWithRationale(rationale);
    }

    if (context != null) {
      context.setState(context.getGameOverState());
    }
  }

  private void finishGameOver() {
    btnGuilty.setDisable(true);
    btnNotGuilty.setDisable(true);
    btnDone.setDisable(true);
    txtaRationale.setDisable(true);

    // Enable the replay button when game is over
    btnReplay.setDisable(false);
    btnReplay.setVisible(true);

    if (verdictTimer != null) {
      verdictTimer.stop();
    }

    isClosed = true;
  }

  private void runGptWithRationale(String rationale) {
    new Thread(
            () -> {
              try {
                Platform.runLater(this::startTypingAnimation); // show typing dots

                String systemPrompt = loadSystemPrompt();
                // Replace both placeholders with actual values
                String mergedPrompt =
                    systemPrompt
                        .replace("{playerRationale}", rationale)
                        .replace("{playerVerdict}", selectedVerdict);

                ChatMessage userMsg = new ChatMessage("user", mergedPrompt);
                chatCompletionRequest.addMessage(userMsg);

                ChatCompletionResult result = chatCompletionRequest.execute();
                Choice choice = result.getChoices().iterator().next();
                ChatMessage aiMsg = choice.getChatMessage();

                chatCompletionRequest.addMessage(aiMsg);

                Platform.runLater(
                    () -> {
                      stopTypingAnimation();
                      txtaFeedback.setText(aiMsg.getContent());
                    });
              } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(
                    () -> {
                      stopTypingAnimation();
                      txtaFeedback.setText("Error getting feedback from GPT.");
                    });
              }
            })
        .start();
  }

  /**
   * Loads the system prompt from the chat_verdictFeedback.txt file for AI feedback generation.
   *
   * @return the system prompt as a string, or a default message if loading fails
   */
  private String loadSystemPrompt() {
    try (InputStream inputStream =
            getClass()
                .getResourceAsStream(
                    "/prompts/chat_verdictFeedback.txt"); // Load prompt from resources
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      return reader
          .lines()
          .collect(Collectors.joining("\n")); // Read all lines into a single string
    } catch (IOException e) {
      e.printStackTrace();
      return "Default prompt"; // Fallback prompt in case of error
    }
  }

  private void startTypingAnimation() {
    txtaFeedback.setText("...");
  }

  private void stopTypingAnimation() {
    // Animation stopped, text will be replaced by actual response
  }

  private void resetGameState() {
    // Clear all chat histories and game state files
    try {
      Files.deleteIfExists(Paths.get("se206/humanWitness_chatHistory.txt"));
      Files.deleteIfExists(Paths.get("se206/humanWitness_revealComment.txt"));
      Files.deleteIfExists(Paths.get("se206/defendant_chatHistory.txt"));
      Files.deleteIfExists(Paths.get("se206/aiWitness_chatHistory.txt"));
      Files.deleteIfExists(Paths.get("se206/k2_chatHistory.txt"));
      Files.deleteIfExists(Paths.get("se206/kalani_chatHistory.txt"));
      Files.deleteIfExists(Paths.get("se206/gregor_chatHistory.txt"));
      Files.deleteIfExists(Paths.get("se206/chat_history_AI-Defendant.txt"));
      Files.deleteIfExists(Paths.get("se206/chat_history_K2.txt"));

      // Clear any other game state files
      Files.deleteIfExists(Paths.get("se206/gameState.txt"));
      Files.deleteIfExists(Paths.get("se206/flashbackState.txt"));
      Files.deleteIfExists(Paths.get("se206/interactionHistory.txt"));

    } catch (IOException e) {
      e.printStackTrace();
    }

    resetControllerStates(); // Reset static states in other controllers

    // IMPORTANT: Reset timer completely before going to room
    System.out.println("Resetting timer service"); // Debugging output
    TimerService.resetInstance(); // Reset the singleton instance
    App.resetContext(); // Reset the global context

    // Go back to the starting scene FIRST
    try {
      App.setRoot("room");

      // THEN start the timer after a short delay
      Platform.runLater(
          () -> {
            System.out.println("Starting new timer after reset"); // Debugging output
            TimerService.getInstance(App.getContext()).start(300); // Start fresh 5-minute timer
          });

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Resets all controller states to their initial values for a fresh game restart. Clears chat
   * histories and resets first-time visit flags.
   */
  private void resetControllerStates() {
    // clears the chat histories of the respective characters.
    AiWitnessMemoryController.clearChatHistory();
    DefendantMemoryMechanismController.clearAllChatHistories();
    HumanWitnessController.resetStaticState();
    // sets the firstTime booleans to true, so that the replay function works properly.
    RoomController.firstTimeHuman = true;
    RoomController.firstTimeAI = true;
    RoomController.firstTimeDefendant = true;
    RoomController.firstTimeRoom = true;
  }
}
