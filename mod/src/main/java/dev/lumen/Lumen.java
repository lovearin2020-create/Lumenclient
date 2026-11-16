package dev.lumen;

import dev.lumen.core.Config;
import dev.lumen.core.HudModule;
import dev.lumen.core.Module;
import dev.lumen.core.ModuleManager;
import dev.lumen.cosmetics.CapeLayer;
import dev.lumen.cosmetics.WingsLayer;
import dev.lumen.gui.HudEditorScreen;
import dev.lumen.gui.ModMenuScreen;
import dev.lumen.util.Clicks;
import dev.lumen.util.R;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = Lumen.MODID, name = Lumen.NAME, version = Lumen.VERSION, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class Lumen {
    public static final String MODID = "lumen", NAME = "Lumen Client", VERSION = "2.1.0";

    public static ModuleManager modules;
    public static final KeyBinding MENU_KEY = new KeyBinding("Open Mod Menu", Keyboard.KEY_RSHIFT, NAME);
    public static final KeyBinding ZOOM_KEY = new KeyBinding("Zoom", Keyboard.KEY_C, NAME);

    private static final Minecraft mc = Minecraft.getMinecraft();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        modules = new ModuleManager();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new Clicks());
        MinecraftForge.EVENT_BUS.register(new dev.lumen.core.Shaders());
        Config.load();
        Config.save(); // make sure the file exists even if the game is closed abruptly
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() { Config.save(); }
        }, "Lumen-ConfigSave"));
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        ClientRegistry.registerKeyBinding(MENU_KEY);
        ClientRegistry.registerKeyBinding(ZOOM_KEY);
        try {
            for (RenderPlayer rp : mc.getRenderManager().getSkinMap().values()) {
                rp.addLayer(new CapeLayer(rp));
                rp.addLayer(new WingsLayer(rp));
            }
        } catch (Throwable t) {
            System.err.println("[Lumen] Cosmetics unavailable: " + t);
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (MENU_KEY.isPressed() && mc.currentScreen == null) mc.displayGuiScreen(new ModMenuScreen());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onOverlay(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.gameSettings.showDebugInfo || mc.currentScreen instanceof HudEditorScreen) return;
        for (Module m : modules.all) {
            if (m.isEnabled() && m instanceof HudModule) ((HudModule) m).render(e.resolution, false);
        }
        R.reset();
    }
}
