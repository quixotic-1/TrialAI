package nz.ac.auckland.se206.states;

import java.io.IOException;
import javafx.scene.input.MouseEvent;
import nz.ac.auckland.se206.GameStateContext;

/**
 * The Guessing state of the game. Handles the logic for when the player is making a guess about the
 * profession of the characters in the game.
 */
public class Guessing implements GameState {

  private final GameStateContext context;
  private boolean verdictPhase = false;

  /**
   * Constructs a new Guessing state with the given game state context.
   *
   * @param context the context of the game state
   */
  public Guessing(GameStateContext context) {
    this.context = context;
  }

  public Guessing(GameStateContext context, boolean verdictPhase) {
    this.context = context;
    this.verdictPhase = verdictPhase;
  }

  @Override
  public void enter() {
    // Allow the main timer to continue running until it naturally reaches 0:00
  }

  /**
   * Handles the event when a rectangle is clicked. Checks if the clicked rectangle is a customer
   * and updates the game state accordingly.
   *
   * @param event the mouse event triggered by clicking a rectangle
   * @param rectangleId the ID of the clicked rectangle
   * @throws IOException if there is an I/O error
   */
  @Override
  public void handleRectangleClick(MouseEvent event, String rectangleId) throws IOException {
    if (verdictPhase) {
      // In verdict phase, ignore rectangle clicks
      event.consume();
      return;
    }

    // Original code for non-verdict phase
    if (rectangleId.equals("rectCashier") || rectangleId.equals("rectWaitress")) {
      return;
    }

    String clickedProfession = context.getProfession(rectangleId);
    if (rectangleId.equals(context.getRectIdToGuess())) {

    } else {

    }
    context.setState(context.getGameOverState());
  }

  /**
   * Handles the event when the guess button is clicked. Since the player has already guessed, it
   * notifies the player.
   *
   * @throws IOException if there is an I/O error
   */
  @Override
  public void handleGuessClick() throws IOException {}
}
