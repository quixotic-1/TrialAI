package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import nz.ac.auckland.se206.App;

public class FlashbackController {

  // Static fields
  private static String participantToShow;

  // Static methods
  /**
   * Sets the participant to show in the flashback sequence.
   *
   * @param participant the name of the participant to display in flashback
   */
  public static void setParticipant(String participant) {
    participantToShow = participant;
  }

  // FXML fields
  @FXML private ImageView imgFlashback;

  // Instance fields
  private int currentIndex = 1;
  private boolean isHuman = false;
  private boolean isAi = false;
  private boolean isDefendant = false;

  // Public methods

  @FXML
  public void initialize() {
    if (participantToShow != null) {
      showFlashback(participantToShow);
    }
  }

  /**
   * Displays flashback content for the specified participant type. Sets up the appropriate
   * flashback sequence based on participant category.
   *
   * @param participant the type of participant ("human", "ai", or "defendant")
   */
  public void showFlashback(String participant) {
    switch (participant.toLowerCase()) {
      case "human":
        isHuman = true;
        showFlashbackHuman(); // Show human flashback
        break;
      case "ai":
        isAi = true;
        showFlashbackAi(); // Show AI flashback
        break;
      case "defendant":
        isDefendant = true;
        showFlashbackDefendant();
        break;
    }
  }

  /**
   * Displays human witness flashback image sequence. Loads the appropriate human participant image
   * based on the current index and displays it with a visual transition effect.
   */
  public void showFlashbackHuman() {
    String imagePath = "/images/human_" + currentIndex + ".png";
    Image humanImage = new Image(getClass().getResourceAsStream(imagePath));
    showImageWithTransition(humanImage);
  }

  /** Displays AI witness flashback image sequence. */
  public void showFlashbackAi() {
    String imagePath = "/images/ai_" + currentIndex + ".png";
    Image aiImage = new Image(getClass().getResourceAsStream(imagePath));
    showImageWithTransition(aiImage);
  }

  /**
   * Displays flashback image specific to the defendant participant type. Configures the image view
   * with defendant-specific flashback imagery and updates display state for defendant flashback
   * sequence.
   */
  public void showFlashbackDefendant() {
    String imagePath = "/images/def_" + currentIndex + ".png";
    Image defImage = new Image(getClass().getResourceAsStream(imagePath));
    showImageWithTransition(defImage);
  }

  /**
   * Event handler that advances to the next flashback image in the sequence. Cycles through
   * available images for the current participant type.
   */
  @FXML
  private void onAdvanceFlashback() {
    currentIndex++;
    System.out.println(currentIndex); // Debugging output to track current index

    if (isHuman) {
      if (currentIndex > 3) {
        goToChat("human-witness"); // Navigate to human witness chat after flashbacks
      } else {
        showFlashbackHuman(); // Show next human flashback image
      }

    } else if (isAi) {
      if (currentIndex > 3) {
        goToChat("aiwitnessmemory"); // Navigate to AI witness chat after flashbacks
      } else {
        showFlashbackAi(); // Show next AI flashback image
      }
    } else if (isDefendant) {
      if (currentIndex > 3) {
        goToChat("defendantMemoryMechanism"); // Navigate to defendant chat after flashbacks
      } else {
        showFlashbackDefendant(); // Show next defendant flashback image
      }
    }
  }

  /**
   * Navigates to the specified participant's chat interface and resets the flashback state. Clears
   * all participant flags and resets the image display for the next session.
   *
   * @param participant the name of the participant scene to navigate to
   */
  private void goToChat(String participant) {
    try {
      App.setRoot(participant);
    } catch (IOException e) {
      System.out.println("doesn't work"); // Error handling for if scene change fails
    }

    currentIndex = 1;
    isHuman = false;
    isAi = false;
    isDefendant = false;
    imgFlashback.setImage(null); // Clear image for next time
  }

  /**
   * Displays a new image with smooth visual transition effects. Creates a slide-out and fade-out
   * animation for the current image, then slides in and fades in the new image to provide a
   * seamless visual transition between flashback images.
   *
   * @param newImage the new image to display with transition effects
   */
  private void showImageWithTransition(Image newImage) {
    // Slide the old image up & fade out
    TranslateTransition slideOut = new TranslateTransition(Duration.millis(400), imgFlashback);
    slideOut.setFromY(0);
    slideOut.setToY(-imgFlashback.getFitHeight() / 2); // move up halfway

    FadeTransition fadeOut = new FadeTransition(Duration.millis(400), imgFlashback);
    fadeOut.setFromValue(1);
    fadeOut.setToValue(0);

    // When that finishes, swap the image
    slideOut.setOnFinished(
        e -> {
          imgFlashback.setImage(newImage);
          imgFlashback.setTranslateY(imgFlashback.getFitHeight() / 2); // start below
          FadeTransition fadeIn = new FadeTransition(Duration.millis(400), imgFlashback);
          fadeIn.setFromValue(0);
          fadeIn.setToValue(1);

          TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), imgFlashback);
          slideIn.setFromY(imgFlashback.getTranslateY());
          slideIn.setToY(0);

          new ParallelTransition(fadeIn, slideIn).play();
        });

    new ParallelTransition(slideOut, fadeOut).play();
  }
}
