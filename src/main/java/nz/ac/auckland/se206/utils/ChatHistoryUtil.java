package nz.ac.auckland.se206.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;

/**
 * Utility class for accessing chat histories across different controllers. Allows AI participants
 * to reference conversations from other participants.
 */
public class ChatHistoryUtil {

  /**
   * Gets all available chat histories from the target directory.
   *
   * @return Map where key is participant name and value is their chat history as a string
   */
  public static Map<String, String> getAllChatHistories() {
    Map<String, String> allHistories = new HashMap<>();

    // Define all possible chat history files
    String[] participants = {"AI-Defendant", "GREGOR", "K2", "KALANI"};

    // Load participant chat histories (format: target/chat_history_{participant}.txt)
    for (String participant : participants) {
      String filename = "target/chat_history_" + participant + ".txt";
      String history = loadChatHistoryFile(filename);
      if (!history.isEmpty()) {
        allHistories.put(participant, history);
      }
    }

    // Load AI witness history (format: target/chat_history_aiwitness.txt)
    String aiWitnessHistory = loadChatHistoryFile("target/chat_history_aiwitness.txt");
    if (!aiWitnessHistory.isEmpty()) {
      allHistories.put("AI-Witness", aiWitnessHistory);
    }

    // Load human witness history (format: target/humanWitness_chatHistory.txt)
    String humanWitnessHistory = loadChatHistoryFile("target/humanWitness_chatHistory.txt");
    if (!humanWitnessHistory.isEmpty()) {
      allHistories.put("Human-Witness", humanWitnessHistory);
    }

    return allHistories;
  }

  /**
   * Gets chat histories for all participants except the current one.
   *
   * @param currentParticipant The name of the current participant to exclude
   * @return Formatted string containing all other participants' chat histories
   */
  public static String getOtherChatHistories(String currentParticipant) {
    Map<String, String> allHistories = getAllChatHistories();
    StringBuilder othersHistory = new StringBuilder();

    for (Map.Entry<String, String> entry : allHistories.entrySet()) {
      String participant = entry.getKey();
      String history = entry.getValue();

      // Skip if this is the current participant
      if (participant.equalsIgnoreCase(currentParticipant)
          || participant.contains(currentParticipant.toUpperCase())
          || currentParticipant.toUpperCase().contains(participant.toUpperCase())) {
        continue;
      }

      if (!history.isEmpty()) {
        othersHistory
            .append("--- CONVERSATION WITH ")
            .append(participant.toUpperCase())
            .append(" ---\n");

        // Parse and reformat the conversation to make roles clear
        String formattedHistory = formatConversationHistory(history, participant);
        othersHistory.append(formattedHistory);

        othersHistory
            .append("--- END OF ")
            .append(participant.toUpperCase())
            .append(" CONVERSATION ---\n\n");
      }
    }

    return othersHistory.toString();
  }

  /**
   * Formats a conversation history to make it clear who said what to whom.
   *
   * @param rawHistory The raw chat history from file
   * @param participantName The name of the participant
   * @return Formatted conversation with clear speaker identification
   */
  private static String formatConversationHistory(String rawHistory, String participantName) {
    StringBuilder formatted = new StringBuilder();
    String[] lines = rawHistory.split("\n");

    for (String line : lines) {
      if (line.trim().isEmpty()) {
        continue;
      }

      if (line.startsWith("user: ")) {
        String userMessage = line.substring(6); // Remove "user: " prefix
        formatted
            .append(">> USER (to ")
            .append(participantName)
            .append("): ")
            .append(userMessage)
            .append("\n");
      } else if (line.startsWith("assistant: ")) {
        String assistantMessage = line.substring(11); // Remove "assistant: " prefix
        formatted
            .append(">> ")
            .append(participantName)
            .append(" (reply): ")
            .append(assistantMessage)
            .append("\n");
      } else if (line.startsWith("[You]: ")) {
        // Handle Human Witness format
        String userMessage = line.substring(7); // Remove "[You]: " prefix
        formatted
            .append(">> USER (to ")
            .append(participantName)
            .append("): ")
            .append(userMessage)
            .append("\n");
      } else if (line.startsWith("Human Witness: ")) {
        // Handle Human Witness format
        String assistantMessage = line.substring(15); // Remove "Human Witness: " prefix
        formatted
            .append(">> ")
            .append(participantName)
            .append(" (reply): ")
            .append(assistantMessage)
            .append("\n");
      } else if (line.contains(": ")) {
        // Handle any other role-based format
        int colonIndex = line.indexOf(": ");
        String role = line.substring(0, colonIndex);
        String message = line.substring(colonIndex + 2);

        if (role.equalsIgnoreCase("user") || role.contains("You")) {
          formatted
              .append("USER (to ")
              .append(participantName)
              .append("): ")
              .append(message)
              .append("\n");
        } else {
          formatted.append(participantName).append(" (reply): ").append(message).append("\n");
        }
      } else {
        // If no clear format, treat as continuation of previous message
        formatted.append("    ").append(line).append("\n");
      }
    }

    return formatted.toString();
  }

  /**
   * Gets a specific participant's chat history.
   *
   * @param participantName The name of the participant whose history to retrieve
   * @return The formatted chat history as a string, or empty string if not found
   */
  public static String getSpecificChatHistory(String participantName) {
    Map<String, String> allHistories = getAllChatHistories(); // Get all histories

    for (Map.Entry<String, String> entry : allHistories.entrySet()) {
      String participant = entry.getKey();
      if (participant.equalsIgnoreCase(participantName)
          || participant.contains(participantName.toUpperCase())
          || participantName.toUpperCase().contains(participant.toUpperCase())) {
        String rawHistory = entry.getValue(); // Get the raw history
        if (!rawHistory.isEmpty()) {
          return formatConversationHistory(rawHistory, participant); // Format and return
        }
      }
    }

    return "";
  }

  /**
   * Loads a chat history file and returns its contents as a string.
   *
   * @param filename The path to the chat history file
   * @return The file contents as a string, or empty string if file doesn't exist or can't be read
   */
  private static String loadChatHistoryFile(String filename) {
    try {
      List<String> lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
      if (lines.isEmpty()) {
        return "";
      }
      return String.join("\n", lines);
    } catch (IOException e) {
      // File doesn't exist or can't be read
      return "";
    }
  }

  /**
   * Checks if any chat histories exist for other participants.
   *
   * @param currentParticipant The current participant to exclude from the check
   * @return true if other participants have chat histories, false otherwise
   */
  public static boolean hasOtherChatHistories(String currentParticipant) {
    return !getOtherChatHistories(currentParticipant).isEmpty();
  }

  /**
   * Enhances a chat message with context from other conversations if it's a user message. This is a
   * utility method to avoid code duplication across controllers.
   *
   * @param msg the original chat message
   * @param participantName the name of the current participant (e.g., "Human-Witness",
   *     "AI-Defendant")
   * @return the enhanced chat message with conversation history context, or the original message if
   *     no enhancement is needed
   */
  public static ChatMessage enhanceMessageWithContext(ChatMessage msg, String participantName) {
    if (!"user".equals(msg.getRole())) {
      return msg; // Only enhance user messages
    }

    String otherHistories =
        getOtherChatHistories(participantName); // Get other participants' histories
    if (otherHistories.isEmpty()) {
      return msg; // No other histories to add
    }

    String enhancedContent =
        msg.getContent()
            + "\n\n"
            + "[CONVERSATION HISTORY REFERENCE: The user has spoken to other participants. When"
            + " asked 'what did I say to [PARTICIPANT]', find the exact conversation section"
            + " with that participant and look for messages starting with 'USER (to"
            + " PARTICIPANT):'. Do NOT confuse different participants' conversations. Each is"
            + " clearly separated.\n\n"
            + otherHistories
            + "IMPORTANT: If asked about what the user said to a specific participant, ONLY"
            + " look in that participant's conversation section. Do not reference other"
            + " participants when answering about a specific one.]";

    return new ChatMessage(msg.getRole(), enhancedContent); // Return new enhanced message
  }
}
