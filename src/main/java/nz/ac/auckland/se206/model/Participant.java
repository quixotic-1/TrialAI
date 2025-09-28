package nz.ac.auckland.se206.model;

public class Participant {
  private final String id; // e.g. "KALANI"
  private final String displayName; // e.g. "Kalani"
  private final String avatarPath; // e.g. "/images/kalani.png"

  public Participant(String id, String displayName, String avatarPath) {
    this.id = id;
    this.displayName = displayName;
    this.avatarPath = avatarPath;
  }

  public String getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getAvatarPath() {
    return avatarPath;
  }
}