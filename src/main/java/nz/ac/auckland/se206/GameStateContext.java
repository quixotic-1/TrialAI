package nz.ac.auckland.se206;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javafx.scene.input.MouseEvent;
import nz.ac.auckland.se206.model.Participant;
import nz.ac.auckland.se206.states.GameOver;
import nz.ac.auckland.se206.states.GameStarted;
import nz.ac.auckland.se206.states.GameState;
import nz.ac.auckland.se206.states.Guessing;
import org.yaml.snakeyaml.Yaml;

/**
 * Context class for managing the state of the game. Handles transitions between different game
 * states and maintains game data such as the professions and rectangle IDs.
 */
public class GameStateContext {
  // Constants and immutable fields
  private final String rectIdToGuess;
  private final String professionToGuess;
  private final Map<String, String> rectanglesToProfession;
  private final GameStarted gameStartedState;
  private final Guessing guessingState;
  private final GameOver gameOverState;

  // State fields
  private GameState gameState;
  private long roundEndEpochMs;
  private long verdictEndEpochMs;
  private boolean roundExpired = false;
  private Boolean playerVerdict = null;
  private boolean flashKalani = false;
  private boolean flashGregor = false;
  private boolean flashK2 = false;
  private String currentTarget = null;
  private Participant selectedParticipant;
  private final Map<String, Boolean> flashbackSeen = new HashMap<>();

  /** Constructs a new GameStateContext and initializes the game states and professions. */
  public GameStateContext() {
    gameStartedState = new GameStarted(this);
    guessingState = new Guessing(this);
    gameOverState = new GameOver(this);

    gameState = gameStartedState; // Initial state
    Map<String, Object> obj = null;
    Yaml yaml = new Yaml();
    try (InputStream inputStream =
        GameStateContext.class.getClassLoader().getResourceAsStream("data/professions.yaml")) {
      if (inputStream == null) {
        throw new IllegalStateException("File not found!");
      }
      obj = yaml.load(inputStream);
    } catch (IOException e) {
      e.printStackTrace();
    }

    @SuppressWarnings("unchecked")
    List<String> professions = (List<String>) obj.get("professions");

    Random random = new Random();
    Set<String> randomProfessions = new HashSet<>();
    while (randomProfessions.size() < 3) {
      String profession = professions.get(random.nextInt(professions.size()));
      randomProfessions.add(profession);
    }

    rectanglesToProfession = new HashMap<>();
    rectanglesToProfession.put("rectKalani", "KALANI");
    rectanglesToProfession.put("rectGregor", "GREGOR");
    rectanglesToProfession.put("rectK2", "K2");

    int randomNumber = random.nextInt(3);
    rectIdToGuess =
        randomNumber == 0 ? "rectKalani" : ((randomNumber == 1) ? "rectGregor" : "rectK2");
    professionToGuess = rectanglesToProfession.get(rectIdToGuess);
  }

  public void setSelectedParticipant(Participant p) {
    this.selectedParticipant = p;
  }

  /**
   * Gets the currently selected participant in the game.
   *
   * @return the selected participant
   */
  public Participant getSelectedParticipant() {
    return selectedParticipant;
  }

  public boolean consumeFirstFlashback(String id) {
    boolean seen = flashbackSeen.getOrDefault(id, false);
    if (!seen) {
      flashbackSeen.put(id, true);
      return true;
    }
    return false;
  }

  /**
   * Sets the current state of the game.
   *
   * @param state the new state to set
   */
  public void setState(GameState state) {
    this.gameState = state;
    state.enter();
  }

  /**
   * Gets the initial game started state.
   *
   * @return the game started state
   */
  public GameState getGameStartedState() {
    return gameStartedState;
  }

  /**
   * Gets the guessing state.
   *
   * @return the guessing state
   */
  public GameState getGuessingState() {
    return guessingState;
  }

  /**
   * Gets the game over state.
   *
   * @return the game over state
   */
  public GameState getGameOverState() {
    return gameOverState;
  }

  /**
   * Gets the profession to be guessed.
   *
   * @return the profession to guess
   */
  public String getProfessionToGuess() {
    return professionToGuess;
  }

  /**
   * Gets the ID of the rectangle to be guessed.
   *
   * @return the rectangle ID to guess
   */
  public String getRectIdToGuess() {
    return rectIdToGuess;
  }

  /**
   * Gets the profession associated with a specific rectangle ID.
   *
   * @param rectangleId the rectangle ID
   * @return the profession associated with the rectangle ID
   */
  public String getProfession(String rectangleId) {
    return rectanglesToProfession.get(rectangleId);
  }

  /**
   * Handles the event when a rectangle is clicked.
   *
   * @param event the mouse event triggered by clicking a rectangle
   * @param rectangleId the ID of the clicked rectangle
   * @throws IOException if there is an I/O error
   */
  public void handleRectangleClick(MouseEvent event, String rectangleId) throws IOException {
    gameState.handleRectangleClick(event, rectangleId);
  }

  /**
   * Handles the event when the guess button is clicked.
   *
   * @throws IOException if there is an I/O error
   */
  public void handleGuessClick() throws IOException {
    gameState.handleGuessClick();
  }

  /**
   * Sets the current target identifier for the game.
   *
   * @param t the target identifier to set
   */
  public void setCurrentTarget(String t) {
    currentTarget = t;
  }

  /**
   * Gets the current target identifier for the game.
   *
   * @return the current target identifier
   */
  public String getCurrentTarget() {
    return currentTarget;
  }

  /**
   * Marks a participant as having shown their flashback sequence. Updates the flashback status for
   * the specified participant to prevent duplicate flashbacks.
   *
   * @param who the participant identifier (KALANI, GREGOR, or K2)
   */
  public void markFlash(String who) {
    if ("KALANI".equals(who)) {
      flashKalani = true;
    } else if ("GREGOR".equals(who)) {
      flashGregor = true;
    } else if ("K2".equals(who)) {
      flashK2 = true;
    }
  }

  /**
   * Checks if a flashback is needed for the specified participant.
   *
   * @param who the participant name (KALANI, GREGOR, or K2)
   * @return true if flashback is needed, false otherwise
   */
  public boolean needsFlash(String who) {
    if ("KALANI".equals(who)) {
      return !flashKalani; // If AI  Defendant's flashback hasn't been shown, return true
    } else if ("GREGOR".equals(who)) {
      return !flashGregor; // If Human Witness's flashback hasn't been shown, return true
    } else if ("K2".equals(who)) {
      return !flashK2; // If Ai Witness's flashback hasn't been shown, return true
    } else {
      return false;
    }
  }

  public long getRoundEndEpochMs() {
    return roundEndEpochMs;
  }

  public long getVerdictEndEpochMs() {
    return verdictEndEpochMs;
  }

  public boolean isRoundExpired() {
    return roundExpired;
  }

  public void setPlayerVerdict(Boolean verdict) {
    this.playerVerdict = verdict;
  }

  public Boolean getPlayerVerdict() {
    return playerVerdict;
  }

  // ---------------- Timer Control Methods ----------------
  public void startRoundTimer() {
    // Comment out or remove this line to disable the 2-minute cutoff:
    // roundEndEpochMs = System.currentTimeMillis() + 120_000L; // 2 minutes
    roundEndEpochMs = 0; // Disable round timer
    roundExpired = false;
  }

  public void onTick() {
    // do nothing
  }

  /**
   * Checks if all three participants have been asked questions by examining their chat history
   * files.
   *
   * @return true if all participants (AI-Defendant, HumanWitness, aiwitness) have been questioned
   */
  public boolean areAllParticipantsQuestioned() {
    return hasUserQuestionDefendant()
        && hasUserQuestionHumanWitness()
        && hasUserQuestionAiWitness();
  }

  /**
   * Checks if the AI-Defendant has been asked a question by looking for "user:" messages in their
   * chat history.
   *
   * @return true if target/chat_history_AI-Defendant.txt contains at least one "user:" message
   */
  private boolean hasUserQuestionDefendant() {
    try {
      Path filePath = Paths.get("target/chat_history_AI-Defendant.txt"); // Corrected filename
      if (!Files.exists(filePath)) {
        return false; // File doesn't exist
      }
      List<String> lines = Files.readAllLines(filePath);
      return lines.stream().anyMatch(line -> line.startsWith("user:")); // Check for "user:" prefix
    } catch (Exception e) {
      return false; // In case of any error, assume no questions have been asked
    }
  }

  /**
   * Checks if the Human Witness has been asked a question by looking for "[You]" messages in their
   * chat history.
   *
   * @return true if target/humanWitness_chatHistory.txt contains at least one "[You]" message
   */
  private boolean hasUserQuestionHumanWitness() {
    try {
      // Checking for the human witness chat history file.
      Path filePath = Paths.get("target/humanWitness_chatHistory.txt");
      if (!Files.exists(filePath)) {
        return false;
      }
      List<String> lines = Files.readAllLines(filePath);
      return lines.stream().anyMatch(line -> line.contains("[You]"));
    } catch (Exception e) {
      return false; // In case of any error, assume no questions have been asked
    }
  }

  /**
   * Checks if the AI Witness has been asked a question by looking for "user:" messages in their
   * chat history.
   *
   * @return true if target/chat_history_aiwitness.txt contains at least one "user:" message
   */
  private boolean hasUserQuestionAiWitness() {
    // Checking if AI witness chat history file exists
    try {
      Path filePath = Paths.get("target/chat_history_aiwitness.txt");
      if (!Files.exists(filePath)) {
        return false;
      }
      List<String> lines = Files.readAllLines(filePath);
      return lines.stream().anyMatch(line -> line.startsWith("user:")); // Check for "user:" prefix
    } catch (Exception e) {
      return false; // In case of any error, assume no questions have been asked
    }
  }
}
