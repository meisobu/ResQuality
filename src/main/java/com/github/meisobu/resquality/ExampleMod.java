package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.io.File;

@Mod(modid = "resquality", name = "ResQuality", version = "1.0", acceptableRemoteVersions = "*")
public class ExampleMod {

    private static File cfgFile;
    private static int targetW = 0, targetH = 0; // 0 = desligado
    private static boolean linear = false;
    private static boolean filterDirty = true;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        cfgFile = e.getSuggestedConfigurationFile();
        Configuration c = new Configuration(cfgFile);
        c.load();
        targetW = c.getInt("width", "general", 0, 0, 16384, "0 = desligado");
        targetH = c.getInt("height", "general", 0, 0, 16384, "0 = desligado");
        linear = c.getBoolean("linear", "general", false, "true = suavizado, false = pixelado");
        c.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(this);
        net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(this);
        ClientCommandHandler.instance.registerCommand(new RqCommand());
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        int w = mc.displayWidth;
int h = mc.displayHeight;
if (targetW > 0) {
    w = targetW;
    h = Math.max(1, Math.round(targetW * (float) mc.displayHeight / mc.displayWidth));
}

        Minecraft mc = Minecraft.getMinecraft();
        Framebuffer fb = mc.getFramebuffer();
        if (fb == null) return;

        int w = targetW > 0 ? targetW : mc.displayWidth;
        int h = targetH > 0 ? targetH : mc.displayHeight;

        if (fb.framebufferWidth != w || fb.framebufferHeight != h) {
            fb.createBindFramebuffer(w, h);
            filterDirty = true;
        }
        if (filterDirty) {
            fb.setFramebufferFilter(linear ? GL11.GL_LINEAR : GL11.GL_NEAREST);
            filterDirty = false;
        }
    }

    private static void save() {
        if (cfgFile == null) return;
        Configuration c = new Configuration(cfgFile);
        c.load();
        c.get("general", "width", 0).set(targetW);
        c.get("general", "height", 0).set(targetH);
        c.get("general", "linear", false).set(linear);
        c.save();
    }

    private static void msg(ICommandSender s, String text) {
        s.addChatMessage(new ChatComponentText("§b[RQ] §f" + text));
    }

    public static class RqCommand extends CommandBase {
        @Override
        public String getCommandName() {
            return "rq";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/rq <largura>x<altura> | /rq off | /rq linear | /rq nearest";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return true;
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) throws CommandException {
            if (args.length == 0) {
                msg(sender, "Uso: " + getCommandUsage(sender));
                return;
            }
            String a = args[0].toLowerCase();

            if (a.equals("off") || a.equals("reset")) {
                targetW = 0;
                targetH = 0;
                save();
                msg(sender, "Resolução nativa restaurada.");
            } else if (a.equals("linear")) {
                linear = true;
                filterDirty = true;
                save();
                msg(sender, "Filtro: suavizado.");
            } else if (a.equals("nearest")) {
                linear = false;
                filterDirty = true;
                save();
                msg(sender, "Filtro: pixelado.");
            } else if (a.matches("\\d{2,5}x\\d{2,5}")) {
                String[] p = a.split("x");
                int w = Integer.parseInt(p[0]);
                int h = Integer.parseInt(p[1]);
                if (w < 64 || h < 64 || w > 8192 || h > 8192) {
                    msg(sender, "Valores fora do limite (64 a 8192).");
                    return;
                }
                targetW = w;
                targetH = h;
                save();
                msg(sender, "Renderizando em " + w + "x" + h + ".");
            } else {
                msg(sender, "Uso: " + getCommandUsage(sender));
            }
        }
    }
}
