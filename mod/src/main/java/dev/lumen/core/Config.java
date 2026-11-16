package dev.lumen.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lumen.Lumen;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static File dir() {
        File d = new File(Minecraft.getMinecraft().mcDataDir, "config/lumen");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private static File file() { return new File(dir(), "config.json"); }

    /** Name of the profile the current settings were last loaded from or saved to ("" = none). */
    public static String activeProfile = "";

    private static File profileDir() {
        File d = new File(dir(), "profiles");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private static String clean(String name) {
        String s = name.replaceAll("[^A-Za-z0-9 _\\-]", "").trim();
        return s.length() > 24 ? s.substring(0, 24) : s;
    }

    /** Saved profile names, alphabetical. Each is a full copy of the settings in config/lumen/profiles/NAME.json. */
    public static java.util.List<String> profiles() {
        java.util.List<String> out = new java.util.ArrayList<String>();
        File[] files = profileDir().listFiles();
        if (files != null) for (File f : files) if (f.isFile() && f.getName().endsWith(".json")) out.add(f.getName().substring(0, f.getName().length() - 5));
        java.util.Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    public static boolean saveProfile(String name) {
        name = clean(name);
        if (name.isEmpty()) return false;
        activeProfile = name;
        write(new File(profileDir(), name + ".json"));
        save();
        return true;
    }

    public static boolean loadProfile(String name) {
        File f = new File(profileDir(), clean(name) + ".json");
        if (!f.exists()) return false;
        read(f);
        activeProfile = clean(name);
        save();
        return true;
    }

    public static void deleteProfile(String name) {
        File f = new File(profileDir(), clean(name) + ".json");
        if (f.exists()) f.delete();
        if (activeProfile.equals(clean(name))) { activeProfile = ""; save(); }
    }

    public static void save() { write(file()); }

    private static void write(File target) {
        try {
            JsonObject root = new JsonObject();
            JsonObject client = new JsonObject();
            client.addProperty("font", dev.lumen.util.Fnt.minecraft ? "minecraft" : "smooth");
            client.addProperty("profile", activeProfile);
            root.add("_client", client);
            for (Module m : Lumen.modules.all) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.isEnabled());
                if (m instanceof HudModule) {
                    HudModule h = (HudModule) m;
                    o.addProperty("ax", h.ax); o.addProperty("ay", h.ay); o.addProperty("ox", h.ox); o.addProperty("oy", h.oy); o.addProperty("scale", h.scale);
                }
                JsonObject s = new JsonObject();
                for (Setting st : m.settings) s.add(st.name, st.save());
                o.add("settings", s);
                root.add(m.name, o);
            }
            Writer w = new OutputStreamWriter(new FileOutputStream(target), "UTF-8");
            try { GSON.toJson(root, w); } finally { w.close(); }
        } catch (Throwable t) {
            System.err.println("[Lumen] Could not save config: " + t);
        }
    }

    public static void load() {
        read(file());
        try {
            JsonObject root = new JsonParser().parse(new String(java.nio.file.Files.readAllBytes(file().toPath()), "UTF-8")).getAsJsonObject();
            activeProfile = root.getAsJsonObject("_client").get("profile").getAsString();
        } catch (Throwable ignored) {}
    }

    private static void read(File f) {
        JsonObject root = null;
        if (f.exists()) {
            try {
                Reader r = new InputStreamReader(new FileInputStream(f), "UTF-8");
                try { root = new JsonParser().parse(r).getAsJsonObject(); } finally { r.close(); }
            } catch (Throwable t) {
                System.err.println("[Lumen] Config unreadable, using defaults: " + t);
            }
        }
        try {
            if (root != null && root.has("_client")) dev.lumen.util.Fnt.minecraft = "minecraft".equals(root.getAsJsonObject("_client").get("font").getAsString());
        } catch (Throwable ignored) {}
        for (Module m : Lumen.modules.all) {
            boolean enabled = Lumen.modules.defaultOn.contains(m);
            if (root != null && root.has(m.name)) {
                try {
                    JsonObject o = root.getAsJsonObject(m.name);
                    if (o.has("enabled")) enabled = o.get("enabled").getAsBoolean();
                    if (m instanceof HudModule) {
                        HudModule h = (HudModule) m;
                        if (o.has("ax")) h.ax = Math.max(0, Math.min(2, o.get("ax").getAsInt()));
                        if (o.has("ay")) h.ay = Math.max(0, Math.min(2, o.get("ay").getAsInt()));
                        if (o.has("ox")) h.ox = o.get("ox").getAsFloat();
                        if (o.has("oy")) h.oy = o.get("oy").getAsFloat();
                        if (o.has("scale")) h.scale = Math.max(0.5f, Math.min(2.5f, o.get("scale").getAsFloat()));
                    }
                    if (o.has("settings")) {
                        JsonObject s = o.getAsJsonObject("settings");
                        for (Setting st : m.settings) {
                            JsonElement e = s.get(st.name);
                            if (e != null) try { st.load(e); } catch (Throwable ignored) {}
                        }
                    }
                } catch (Throwable ignored) {}
            }
            m.setEnabled(enabled);
        }
    }
}
