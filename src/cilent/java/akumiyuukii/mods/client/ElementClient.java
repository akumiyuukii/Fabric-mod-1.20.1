package akumiyuukii.mods.client;

import akumiyuukii.mods.Element;

/**
 * Client-side holder for the player's element state, synced from the server.
 */
public class ElementClient {
    public static Element element = Element.PHYSICAL;
    public static boolean hasChosen = false;
    public static int points = 0;

    public static void update(int ordinal, boolean hasChosen, int points) {
        ElementClient.element = Element.byOrdinal(ordinal);
        ElementClient.hasChosen = hasChosen;
        ElementClient.points = points;
    }
}
