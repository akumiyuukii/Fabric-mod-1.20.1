package akumiyuukii.mods;

/**
 * The six damage elements. A player has exactly one element (chosen on first join); all damage
 * they deal is of that element. Each element has a signature on-hit proc.
 */
public enum Element {
    PHYSICAL("physical", "Vật lí",   0xFFCCCCCC, "❖", "Xuyên giáp: bỏ qua 15% giáp của mục tiêu."),
    ICE     ("ice",      "Băng",     0xFF66DDFF, "❄", "Đóng băng: có tỉ lệ làm chậm & giữ chân mục tiêu."),
    FIRE    ("fire",     "Lửa",      0xFFFF5533, "🔥", "Thiêu đốt: có tỉ lệ gây bỏng, sát thương theo thời gian."),
    WIND    ("wind",     "Gió",      0xFF66FF99, "🌀", "Xuyên phong: có tỉ lệ gây thêm sát thương gió cắt (nhân đôi)."),
    LIGHTNING("lightning","Sét",     0xFFFFEE55, "⚡", "Cảm điện: có tỉ lệ lan sát thương sang kẻ địch xung quanh."),
    QUANTUM ("quantum",  "Lượng tử", 0xFFCC66FF, "✦", "Vướng víu: có tỉ lệ gây chí mạng lượng tử (x2 sát thương).");

    public final String id;
    public final String displayName;
    public final int color;      // ARGB
    public final String symbol;
    public final String perk;

    Element(String id, String displayName, int color, String symbol, String perk) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.symbol = symbol;
        this.perk = perk;
    }

    public static Element byOrdinal(int ordinal) {
        Element[] values = values();
        if (ordinal < 0 || ordinal >= values.length) return PHYSICAL;
        return values[ordinal];
    }

    public static Element byId(String id) {
        for (Element e : values()) {
            if (e.id.equals(id)) return e;
        }
        return PHYSICAL;
    }
}
