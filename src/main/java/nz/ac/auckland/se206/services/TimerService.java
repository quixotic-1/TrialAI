package nz.ac.auckland.se206.services;

import java.io.IOException;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.util.Duration;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.GameStateContext;

public final class TimerService {
  private static TimerService INSTANCE;

  public static TimerService getInstance(GameStateContext context) {
    if (INSTANCE == null) {
      INSTANCE = new TimerService(context);
    }
    // DON'T update context if timer is already running to prevent conflicts
    if (!INSTANCE.running) {
      INSTANCE.context = context;
    }
    return INSTANCE;
  }

  /**
   * Resets the singleton instance by stopping the current timer and setting instance to null. This
   * allows a fresh timer to be created on the next getInstance() call.
   */
  public static void resetInstance() {
    if (INSTANCE != null) {
      INSTANCE.forceStop();
      INSTANCE = null;
    }
  }

  private GameStateContext context;
  private final IntegerProperty remainingSeconds = new SimpleIntegerProperty(300);
  private Timeline timeline;
  private boolean running = false;

  private TimerService(GameStateContext context) {
    this.context = context;
  }

  public void start(int seconds) {
    if (running) {
      forceStop(); // Stop any existing timer first
    }

    System.out.println("Starting timer with " + seconds + " seconds");
    remainingSeconds.set(seconds);
    running = true;

    timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
    timeline.setCycleCount(Animation.INDEFINITE);
    timeline.play();
  }

  public void stop() {
    if (timeline != null) {
      timeline.stop();
    }
    running = false;
    System.out.println("Timer stopped");
  }

  private void forceStop() {
    if (timeline != null) {
      timeline.stop();
      timeline = null;
    }
    running = false;
    System.out.println("Timer force stopped");
  }

  public boolean isRunning() {
    return running;
  }

  /**
   * Handles a single timer tick. Updates the context if available and decrements the remaining
   * time. When timer reaches zero, triggers the timer end event. This method is called every second
   * while the timer is running.
   */
  private void tick() {
    if (!running) {
      return; // Safety check
    }

    try {
      if (context != null) {
        context.onTick(); // Notify context of tick
      }
    } catch (Exception e) {
      System.err.println("Error in context.onTick(): " + e.getMessage()); // Log error
    }

    int current = remainingSeconds.get(); // Get current remaining seconds
    int next = current - 1; // Decrement by one

    if (next <= 0) {
      remainingSeconds.set(0); // Ensure it doesn't go negative
      stop(); // Stop the timer
      onTimerEnd(); // Handle timer expiration
    } else {
      remainingSeconds.set(next); // Update remaining seconds
    }
  }

  /**
   * Handles timer expiration by checking if all participants were questioned. If not all
   * participants were questioned, goes directly to lose state. Otherwise, transitions to the
   * verdict screen normally.
   */
  private void onTimerEnd() {
    System.out.println("Timer ended - checking if all participants questioned"); // Debug log
    Platform.runLater(
        () -> {
          try {
            // Check if all participants have been questioned
            boolean allQuestioned = context.areAllParticipantsQuestioned();

            if (!allQuestioned) {
              System.out.println("Not all participants questioned - going to lose state");
              // Set context to game over state (lose)
              context.setState(context.getGameOverState()); // Set to lose state
              // Go to verdict with automatic lose
              App.setRoot("verdict");
              // Set a flag or property to indicate automatic lose
              setAutomaticLose();
            } else {
              System.out.println("All participants questioned - going to normal verdict");
              // Normal flow - go to verdict scene
              App.setRoot("verdict");
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
  }

  private void setAutomaticLose() {
    System.setProperty(
        "automaticLose",
        "true"); // Sets a system property to indicate this is an automatic lose scenario
  }

  public IntegerProperty remainingSecondsProperty() {
    return remainingSeconds;
  }

  /**
   * Creates a string binding that formats the remaining time as MM:SS for display in UI.
   *
   * @return a StringBinding that automatically updates when the timer changes
   */
  public StringBinding timeLeftTextBinding() {
    return new StringBinding() {
      {
        super.bind(remainingSeconds); // Bind to remainingSeconds property
      }

      @Override
      protected String computeValue() {
        int total = remainingSeconds.get(); // Get current remaining seconds
        int m = total / 60; // Calculate minutes
        int s = total % 60; // Calculate seconds
        return String.format("%02d:%02d", m, s); // Format as MM:SS
      }
    };
  }
}
