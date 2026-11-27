package com.github.meisobu.resquality;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
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
    private static int quality = 100; // 100 = nativo
    private static boolean linear = false;
    private static boolean filterDirty = true;

    // tamanho real da janela, guardado durante o frame
    private int realW, realH;
    private boolean overridden = false;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        cfgFile = e.getSuggestedConfigurationFile();
        Configuration c = new Configuration(cfgFile);
        c.load();
        quality = c.getInt("quality", "general", 100, 10, 100, "10 a 100 (100 = nativo)");
        linear = c.getBoolean("linear", "general", false, "true = suavizado, false = pixelado");
        c.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        FMLCommonHandler.instance().bus().register(this);
        ClientCommandHandler.instance.registerCommand(new RqCommand());
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        Minecraft mc = Minecraft.getMinecraft();

        if (e.phase == TickEvent.Phase.START) {
            overridden = false;
            if (!OpenGlHelper.isFramebufferEnabled()) return;
            Framebuffer fb = mc.getFramebuffer();
            if (fb == null) return;

            int rw = mc.displayWidth;
            int rh = mc.displayHeight;

            // com tela aberta (menu, inventario, chat) usa resolucao nativa
            boolean reduce = quality < 100 && mc.currentScreen == null;
            int w = rw;
            int h = rh;
            if (reduce) {
                w = Math.max(64, Math.round(rw * quality / 100f));
                h = Math.max(64, Math.round(rh * quality / 100f));
            }

            if (fb.framebufferWidth != w || fb.framebufferHeight != h) {
                fb.createBindFramebuffer(w, h);
                filterDirty = true;
            }
            if (filterDirty) {
                fb.setFramebufferFilter(linear ? GL11.GL_LINEAR : GL11.GL_NEAREST);
                filterDirty = false;
            }

            if (reduce) {
                realW = rw;
                realH = rh;
                mc.displayWidth = w;
                mc.displayHeight = h;
                overridden = true;
            }
        } else if (e.phase == TickEvent.Phase.END) {
            if (overridden) {
                mc.displayWidth = realW;
                mc.displayHeight = realH;
                overridden = false;
            }
        }
    }

    private static void save() {
        if (cfgFile == null) return;
        Configuration c = new Configuration(cfgFile);
        c.load();
        c.get("general", "quality", 100).set(quality);
        c.get("general", "linear", false).set(linear);
        c.save();
    }

    private static void msg(ICommandSender s, String text) {
        s.addChatMessage(new ChatComponentText("\u00a7b[RQ] \u00a7f" + text));
    }

    public static class RqCommand extends CommandBase {
        @Override
        public String getCommandName() {
            return "rq";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/rq <10-100> | /rq off | /rq linear | /rq nearest";
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
                msg(sender, "Qualidade atual: " + quality + ". Uso: " + getCommandUsage(sender));
                return;
            }
            String a = args[0].toLowerCase();

            if (a.equals("off") || a.equals("reset")) {
                quality = 100;
                save();
                msg(sender, "Qualidade 100 (nativa).");
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
            } else if (a.matches("\\d{1,3}")) {
                int q = Integer.parseInt(a);
                if (q > 100) q = 100;
                if (q < 10) q = 10; // abaixo de 10 fica injogavel
                quality = q;
                save();
                msg(sender, "Qualidade: " + q + (q < 100 ? " (sai da tela aberta = nativa)" : ""));
            } else {
                msg(sender, "Uso: " + getCommandUsage(sender));
            }
        }
    }
}
