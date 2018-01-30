package net.insomniakitten.highlighted;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumType;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
@Mod(modid = Highlighted.ID, name = Highlighted.NAME, version = Highlighted.VERSION, clientSideOnly = true)
@Mod.EventBusSubscriber(modid = Highlighted.ID, value = Side.CLIENT)
public final class Highlighted {

    public static final String ID = "highlighted";
    public static final String NAME = "Highlighted";
    public static final String VERSION = "%VERSION%";

    private static final VertexFormat FORMAT = new VertexFormat()
            .addElement(DefaultVertexFormats.POSITION_3F)
            .addElement(DefaultVertexFormats.COLOR_4UB)
            .addElement(new VertexFormatElement(0, EnumType.SHORT, EnumUsage.UV, 2));

    @SubscribeEvent
    public static void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        if (event.getTarget() != null && event.getTarget().typeOfHit == Type.BLOCK) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = FMLClientHandler.instance().getClient();
        RayTraceResult result = mc.objectMouseOver;
        if (result == null) return;
        if (mc.isGamePaused()) return;
        if (result.typeOfHit == Type.BLOCK) {
            drawBlockHighlight(mc, result.getBlockPos(), event.getPartialTicks());
        } else if (result.typeOfHit == Type.ENTITY && ModConfig.highlightEntities) {
            drawEntityHighlight(result.entityHit, event.getPartialTicks());
        }
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (ID.equals(event.getModID())) ConfigManager.sync(ID, Config.Type.INSTANCE);
    }

    private static void drawBlockHighlight(Minecraft mc, BlockPos pos, float partialTicks) {
        World world = mc.player.world;
        IBlockState state = world.getBlockState(pos);

        float red = 0.01F * ModConfig.highlightRed;
        float green = 0.01F * ModConfig.highlightGreen;
        float blue = 0.01F * ModConfig.highlightBlue;
        float alpha = 0.01F * ModConfig.highlightAlpha;

        double offsetX = mc.player.lastTickPosX + (mc.player.posX - mc.player.lastTickPosX) * partialTicks;
        double offsetY = mc.player.lastTickPosY + (mc.player.posY - mc.player.lastTickPosY) * partialTicks;
        double offsetZ = mc.player.lastTickPosZ + (mc.player.posZ - mc.player.lastTickPosZ) * partialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.doPolygonOffset(-1.0F, -1.0F);
        GlStateManager.enablePolygonOffset();

        GlStateManager.enableCull();
        GlStateManager.color(red, green, blue, alpha);

        GlStateManager.tryBlendFuncSeparate(
                SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
                SourceFactor.ONE, DestFactor.ZERO
        );

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();
        BlockModelRenderer renderer = dispatcher.getBlockModelRenderer();
        IBakedModel model = dispatcher.getModelForState(state.getActualState(world, pos));

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        buffer.setTranslation(-offsetX, -offsetY, -offsetZ);

        renderer.renderModel(world, model, state, pos, buffer, true);

        buffer.setTranslation(0.0D, 0.0D, 0.0D);
        tessellator.draw();

        GlStateManager.doPolygonOffset(0.0F, 0.0F);
        GlStateManager.disablePolygonOffset();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void drawEntityHighlight(Entity entity, float partialTicks) {
        float red = 0.01F * ModConfig.highlightRed;
        float green = 0.01F * ModConfig.highlightGreen;
        float blue = 0.01F * ModConfig.highlightBlue;
        float alpha = 0.01F * ModConfig.highlightAlpha;

        Minecraft mc = FMLClientHandler.instance().getClient();

        if (entity.ticksExisted == 0) {
            entity.lastTickPosX = entity.posX;
            entity.lastTickPosY = entity.posY;
            entity.lastTickPosZ = entity.posZ;
        }

        double offsetX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double offsetY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double offsetZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

        double renderX = offsetX - mc.getRenderManager().renderPosX;
        double renderY = offsetY - mc.getRenderManager().renderPosY;
        double renderZ = offsetZ - mc.getRenderManager().renderPosZ;

        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.color(red, green, blue, alpha);

        mc.getRenderManager().setRenderOutlines(true);
        mc.getRenderManager().renderEntity(entity, renderX, renderY, renderZ, yaw, partialTicks, false);
        mc.getRenderManager().setRenderOutlines(false);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    @Config(modid = ID, name = ID)
    public static final class ModConfig {
        @Config.Name("red")
        @Config.Comment("The red percentage to use for the highlight")
        @Config.RangeInt(min = 0, max = 100)
        public static int highlightRed = 100;

        @Config.Name("green")
        @Config.Comment("The green percentage to use for the highlight")
        @Config.RangeInt(min = 0, max = 100)
        public static int highlightGreen = 100;

        @Config.Name("blue")
        @Config.Comment("The blue percentage to use for the highlight")
        @Config.RangeInt(min = 0, max = 100)
        public static int highlightBlue = 100;

        @Config.Name("alpha")
        @Config.Comment("The alpha percentage to use for the highlight")
        @Config.RangeInt(min = 0, max = 100)
        public static int highlightAlpha = 20;

        @Config.Name("highlight_entities")
        @Config.Comment("Should entities be highlighted on mouse-over?")
        public static boolean highlightEntities = true;
    }

}
