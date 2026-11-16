package dev.lumen.core;

public enum Category {
    HUD("HUD"), HYPIXEL("Hypixel"), VISUAL("Visual"), COSMETICS("Cosmetics"), PERFORMANCE("Performance");

    public final String label;
    Category(String label) { this.label = label; }
}
