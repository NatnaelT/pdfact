package pdfact.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The available types of PDF elements.
 *
 * @author Claudius Korzen
 */
public enum PdfElementType {
  /**
   * The type of paragraphs.
   */
  PARAGRAPH("paragraphs"),

  /**
   * The type of text blocks.
   */
  TEXT_BLOCK("blocks"),

  /**
   * The type of text lines.
   */
  TEXT_LINE("lines"),

  /**
   * The type of words.
   */
  WORD("words"),

  /**
   * The type of characters.
   */
  CHARACTER("characters"),

  /**
   * The type of figures.
   */
  FIGURE("figures"),

  /**
   * The type of shapes.
   */
  SHAPE("shapes");

  /**
   * The group name of this type.
   */
  String groupName;

  /**
   * Creates a new type.
   * 
   * @param groupName
   *        The group name of this type.
   */
  PdfElementType(String groupName) {
    this.groupName = groupName;
  }

  /**
   * Returns the group name of this type.
   * 
   * @return The group name of this type.
   */
  public String getGroupName() {
    return this.groupName;
  }

  /**
   * The types per group names.
   */
  protected static Map<String, PdfElementType> index;

  static {
    index = new HashMap<>();

    // Fill the map of types per group name.
    for (PdfElementType type : values()) {
      index.put(type.getGroupName(), type);
    }
  }

  /**
   * Returns a set of the group names of all types.
   * 
   * @return A set of the group names of all types.
   */
  public static Set<String> getGroupNames() {
    return index.keySet();
  }

  /**
   * Checks if the given group name is a group name of an existing type.
   * 
   * @param groupName
   *        The group name to check.
   * 
   * @return True, if the given group name is a group name of an existing type.
   */
  public static boolean isValidType(String groupName) {
    return index.containsKey(groupName.toLowerCase());
  }

  /**
   * Returns the types that are associated with the given group names.
   * 
   * @param groupNames
   *        The group names of the types to fetch.
   *
   * @return A set of the fetched types.
   */
  public static Set<PdfElementType> getTypes(String... groupNames) {
    if (groupNames == null || groupNames.length == 0) {
      return null;
    }

    Set<PdfElementType> types = new HashSet<>();
    for (String name : groupNames) {
      PdfElementType type = getType(name);
      if (type != null) {
        types.add(type);
      }
    }
    return types;
  }

  /**
   * Returns the type that is associated with the given group name.
   * 
   * @param groupName
   *        The group name of the type to fetch.
   *
   * @return The type that is associated with the given group name.
   */
  public static PdfElementType getType(String groupName) {
    if (!isValidType(groupName)) {
      throw new IllegalArgumentException(groupName + " isn't a valid type.");
    }
    return index.get(groupName.toLowerCase());
  }
}