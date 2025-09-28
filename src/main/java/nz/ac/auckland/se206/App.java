package nz.ac.auckland.se206;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import nz.ac.auckland.se206.controllers.DefendantMemoryMechanismController;
import nz.ac.auckland.se206.controllers.RoomController;
import nz.ac.auckland.se206.services.TimerService;

/**
 * This is the entry point of the JavaFX application. This class initializes and runs the JavaFX
 * application.
 */
public class App extends Application {
  private static Scene scene;
  private static GameStateContext globalContext;

  public static GameStateContext getContext() {
    if (globalContext == null) {
      globalContext = new GameStateContext();
    }
    return globalContext;
  }

  /** Resets the global context by creating a new GameStateContext instance. */
  public static void resetContext() {
    globalContext = new GameStateContext();
  }

  /**
   * The main method that launches the JavaFX application.
   *
   * @param args the command line arguments
   */
  public static void main(final String[] args) {
    launch();
  }

  /**
   * Sets the root of the scene to the specified FXML file.
   *
   * @param fxml the name of the FXML file (without extension)
   * @throws IOException if the FXML file is not found
   */
  public static void setRoot(String fxml) throws IOException {
    System.out.println("App.setRoot called with: " + fxml);
    FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml"));
    Parent root = loader.load();

    // If switching to the room, set the context on the controller
    if ("room".equals(fxml)) {
      System.out.println("Setting up room controller with context");
      RoomController room = loader.getController();
      room.setContext(globalContext);
      scene.setRoot(root);
      scene.getProperties().put("controller", room);
    } else {
      System.out.println("Setting root for non-room FXML");
      scene.setRoot(root);
      scene.getProperties().put("controller", loader.getController());
    }
    System.out.println("App.setRoot completed successfully");
  }

  /**
   * Loads the FXML file and returns the associated node. The method expects that the file is
   * located in "src/main/resources/fxml".
   *
   * <p>/** Opens the chat view and sets the profession in the chat controller.
   *
   * @param event the mouse event that triggered the method
   * @param profession the profession to set in the chat controller
   * @throws IOException if the FXML file is not found
   */
  public static void openChat(MouseEvent event, String who) throws IOException {
    // Special case: if clicking on K2, go to AI witness memory interface
    if ("K2".equals(who)) {
      setRoot("aiwitnessmemory");
      return;
    }

    FXMLLoader loader =
        new FXMLLoader(
            App.class.getResource("src/main/resources/fxml/defendantMemoryMechanism.fxml"));
    Parent chatRoot = loader.load();

    DefendantMemoryMechanismController chat = loader.getController();
    chat.setProfession(who);
    System.out.println("testing if this is working");

    scene.setRoot(chatRoot);
    scene.getProperties().put("controller", chat);
  }

  /**
   * This method is invoked when the application starts. It loads and shows the "room" scene.
   *
   * @param stage the primary stage of the application
   * @throws IOException if the "src/main/resources/fxml/room.fxml" file is not found
   */
  @Override
  public void start(final Stage stage) throws IOException {
    // Initialize the global context FIRST
    globalContext = new GameStateContext();

    // Clear chat histories at the start of the game
    DefendantMemoryMechanismController.clearAllChatHistories();
    nz.ac.auckland.se206.controllers.AiWitnessMemoryController.clearChatHistory();

    FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/room.fxml"));
    Parent root = loader.load();

    RoomController room = loader.getController();
    room.setContext(globalContext);

    scene = new Scene(root);
    stage.setScene(scene);
    stage.show();

    globalContext.setState(globalContext.getGameStartedState());
    root.requestFocus();

    // Start the timer AFTER everything is loaded
    Platform.runLater(
        () -> {
          System.out.println("Starting initial timer");
          TimerService.getInstance(globalContext).start(300);
        });
  }

  /**
   * Redirects to the room scene if currently in a chat scene. Checks if the current controller is
   * DefendantMemoryMechanismController and navigates back to room.
   */
  public static void redirectToRoomIfInChat() {
    if (scene != null && scene.getRoot() != null) {
      Object controller = scene.getProperties().get("controller"); // Retrieve the controller
      if (controller instanceof DefendantMemoryMechanismController) {
        try {
          setRoot("room"); // Change back to room scene
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Override
  public void stop() throws Exception {
    // Delete the chat history when the program exits. To do: add other chat history files.
    try {
      Files.deleteIfExists(Paths.get("target/humanWitness_chatHistory.txt"));
      Files.deleteIfExists(Paths.get("se206/humanWitness_revealComment.txt"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
