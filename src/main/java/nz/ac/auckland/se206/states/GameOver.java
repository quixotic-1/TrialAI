package nz.ac.auckland.se206.states;

import java.io.IOException;
import javafx.scene.input.MouseEvent;
import nz.ac.auckland.se206.GameStateContext;

public class GameOver implements GameState {

  private final GameStateContext context;

  public GameOver(GameStateContext context) {
    this.context = context;
  }

  @Override
  public void enter() {
    // Do nothing
  }

  @Override
  public void handleRectangleClick(MouseEvent event, String rectangleId) throws IOException {
    // Do nothing when rectangles are clicked in game over state
    event.consume(); // Consume the event to prevent further processing
  }

  @Override
  public void handleGuessClick() throws IOException {
    // Do nothing when guess button is clicked in game over state
  }
}
