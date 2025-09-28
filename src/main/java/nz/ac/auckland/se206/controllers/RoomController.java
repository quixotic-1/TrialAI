package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Rectangle;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.GameStateContext;
import nz.ac.auckland.se206.services.TimerService;

/**
 * Controller class for the room view. Handles user interactions within the room where the user can
 * chat with customers and guess their profession.
 */
public class RoomController {
  // Static fields first
  static boolean firstTimeHuman = true;
  static boolean firstTimeAI = true;
  static boolean firstTimeDefendant = true;
  static boolean firstTimeRoom = true; // Track if it's the first time in the room (for TTS)
  // Instance fields next - FXML UI elements
  @FXML private Pane root;
  @FXML private ImageView backgroundImage;
  @FXML private Pane hotspotsPane;
  @FXML private Rectangle rectKalani;
  @FXML private Rectangle rectGregor;
  @FXML private Rectangle rectK2;
  @FXML private Label lblProfession;
  @FXML private Button btnGuess;
  @FXML private Label timerLabel;
  @FXML private ImageView imgAvatar;
  @FXML private Label lblName;

  // Non-FXML fields
  private GameStateContext context;
  private MediaPlayer ttsMediaPlayer; // Keep reference to prevent garbage collection

  // Instance methods
  public void setContext(GameStateContext context) {
    this.context = context;

    // any labels that read from context:
    if (lblProfession != null) {
      String p = context.getProfessionToGuess();
      lblProfession.setText(p != null ? p : "");
    }

    // Bind timerLabel to TimerService
    if (timerLabel != null) {
      timerLabel
          .textProperty()
          .bind(TimerService.getInstance(App.getContext()).timeLeftTextBinding());
    }

    // Update verdict button state when context is set
    updateVerdictButtonState();
  }

  /**
   * Updates the verdict button (btnGuess) state based on whether all participants have been
   * questioned. Disables the button if not all participants have been asked questions.
   */
  private void updateVerdictButtonState() {
    if (btnGuess != null && context != null) {
      boolean allQuestioned = context.areAllParticipantsQuestioned();
      btnGuess.setDisable(!allQuestioned);

      // Optional: Update button text to provide feedback
      if (allQuestioned) {
        btnGuess.setText("Make Verdict");
        btnGuess.setStyle(
            "-fx-background-color: #FF6B35; -fx-base: #FF6B35; -fx-text-fill: #FFFFFF;"
                + " -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10px;"
                + " -fx-border-color: #D4522A; -fx-border-width: 2px; -fx-border-radius: 10px;"
                + " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 8, 0, 0, 4);"
                + " -fx-background-insets: 0; -fx-padding: 5px 15px;"); // Apply our custom styling
      } else {
        btnGuess.setText("Question All Participants First");
        btnGuess.setStyle(
            "-fx-background-color: #FF6B35; -fx-base: #FF6B35; -fx-text-fill: #FFFFFF;"
                + " -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10px;"
                + " -fx-border-color: #D4522A; -fx-border-width: 2px; -fx-border-radius: 10px;"
                + " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 8, 0, 0, 4);"
                + " -fx-background-insets: 0;"
                + " -fx-padding: 5px 15px; -fx-opacity:"
                + " 0.6;"); // Apply our custom styling with opacity
      }
    }
  }

  /**
   * Initializes the room view. If it's the first time initialization, it will provide instructions
   * via text-to-speech.
   */
  @FXML
  public void initialize() {

    // Initially disable the verdict button until all participants are questioned
    if (btnGuess != null) {
      btnGuess.setDisable(true);
      btnGuess.setText("Question All Participants First");
      btnGuess.setStyle(
          "-fx-background-color: #FF6B35; -fx-base: #FF6B35; -fx-text-fill: #FFFFFF; -fx-font-size:"
              + " 14px; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-border-color:"
              + " #D4522A; -fx-border-width: 2px; -fx-border-radius: 10px; -fx-effect:"
              + " dropshadow(three-pass-box, rgba(0,0,0,0.5), 8, 0, 0, 4); -fx-background-insets:"
              + " 0; -fx-padding: 5px 15px; -fx-opacity: 0.6;");
    }

    // Properly formatted empty block
    if (root != null && backgroundImage != null) {
      // Intentionally empty
    }

    if (root != null && hotspotsPane != null) {
      hotspotsPane.setLayoutX(0);
      hotspotsPane.setLayoutY(0);
    }

    // Play the TTS audio instruction only on first visit to room or after restart
    if (firstTimeRoom) {
      firstTimeRoom = false;
      playTextToSpeechAudio();
    }
  }

  /**
   * Plays the prerecorded TTS audio file when the game starts or restarts. This provides audio
   * instruction to the player about the game objective.
   */
  private void playTextToSpeechAudio() {
    try {
      // Stop any previous audio if still playing
      if (ttsMediaPlayer != null) {
        ttsMediaPlayer.stop();
        ttsMediaPlayer.dispose();
      }

      // Load the TTS audio file from resources
      String audioPath = getClass().getResource("/sounds/tts.mp3").toExternalForm();
      Media media = new Media(audioPath);
      ttsMediaPlayer = new MediaPlayer(media);

      // Set up event handler to clean up when finished
      ttsMediaPlayer.setOnEndOfMedia(
          () -> {
            ttsMediaPlayer.dispose();
            ttsMediaPlayer = null;
          });

      // Set up error handler
      ttsMediaPlayer.setOnError(
          () -> {
            System.err.println("MediaPlayer error: " + ttsMediaPlayer.getError().getMessage());
            if (ttsMediaPlayer != null) {
              ttsMediaPlayer.dispose();
              ttsMediaPlayer = null;
            }
          });

      // Play the audio
      ttsMediaPlayer.play();

    } catch (Exception e) {
      System.err.println("Error playing TTS audio: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Handles the key pressed event.
   *
   * @param event the key event
   */
  @FXML
  public void onKeyPressed(KeyEvent event) {
    System.out.println("Key " + event.getCode() + " pressed");
  }

  /**
   * Handles the key released event.
   *
   * @param event the key event
   */
  @FXML
  public void onKeyReleased(KeyEvent event) {
    System.out.println("Key " + event.getCode() + " released");
  }

  /**
   * Handles mouse clicks on rectangles representing people in the room.
   *
   * @param event the mouse event triggered by clicking a rectangle
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void onRectangleClick(MouseEvent event) throws IOException {

    Object src = event.getSource();
    String fxmlName = null;

    if (src == rectKalani) {
      // Kalani rectangle -> AI defendant
      if (firstTimeDefendant) {
        firstTimeDefendant = false;
        FlashbackController.setParticipant("defendant");
        App.setRoot("flashback");
        return;
      } else {

        fxmlName = "defendantMemoryMechanism";
      }

    } else if (src == rectGregor) {
      // Gregor rectangle -> Human witness
      if (firstTimeHuman) {
        firstTimeHuman = false;
        FlashbackController.setParticipant("human");
        App.setRoot("flashback");
        return;
      } else {

        fxmlName = "human-witness";
      }

    } else if (src == rectK2) {
      // K2 rectangle -> AI witness
      if (firstTimeAI) {
        firstTimeAI = false;
        FlashbackController.setParticipant("ai");
        App.setRoot("flashback");
        return;
      } else {

        fxmlName = "aiwitnessmemory";
      }
    }

    if (fxmlName != null) {
      System.out.println("RoomController: Navigating to " + fxmlName);
      App.setRoot(fxmlName);
      return;
    } else {
      System.out.println("failing to change scenes");
    }

    String id = ((Rectangle) event.getSource()).getId(); // use imported Rectangle type
    if (id == null) {
      return;
    }
    App.openChat(event, id);
  }

  /**
   * Handles the guess button click event.
   *
   * @param event the action event triggered by clicking the guess button
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void onGuessClick(ActionEvent event) throws IOException {
    // Stop the 5-minute timer when switching to verdict scene
    TimerService.getInstance(App.getContext()).stop();
    // Switch to verdict.fxml when guess button is clicked
    App.setRoot("verdict");
  }

  @FXML
  private void onParticipantClicked(MouseEvent e) throws IOException {
    String id = ((Rectangle) e.getSource()).getId(); // use imported Rectangle type
    if (id == null) {
      return;
    }
    // Set the participant in context
    // Example: context.setCurrentTarget(id);
    App.openChat(e, id);
  }
}
