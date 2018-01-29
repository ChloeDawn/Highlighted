package net.insomniakitten.highlighted;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumType;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.IBlockAccess;
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

import java.util.ArrayList;
import java.util.List;

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
            drawBlockHighlight(mc.player, result.getBlockPos(), result.sideHit, event.getPartialTicks());
        } else if (result.typeOfHit == Type.ENTITY && ModConfig.highlightEntities) {
            drawEntityHighlight(result.entityHit, event.getPartialTicks());
        }
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (ID.equals(event.getModID())) ConfigManager.sync(ID, Config.Type.INSTANCE);
    }

    private static void drawBlockHighlight(EntityPlayer player, BlockPos pos, EnumFacing side, float partialTicks) {
        World world = player.world;
        IBlockState state = world.getBlockState(pos);
        List<AxisAlignedBB> boxes = new ArrayList<>();

        state.addCollisionBoxToList(world, pos, Block.FULL_BLOCK_AABB.offset(pos), boxes, player, false);
        if (boxes.isEmpty()) boxes.add(state.getSelectedBoundingBox(world, pos));

        double offsetX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double offsetY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double offsetZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.enableCull();
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(ModConfig.renderMode.glMode, FORMAT);
        buffer.setTranslation(-offsetX, -offsetY, -offsetZ);

        ModConfig.renderMode.builder.accept(buffer, state, world, pos, side, boxes);

        buffer.setTranslation(0.0D, 0.0D, 0.0D);
        tessellator.draw();

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
    }

    private static void drawEntityHighlight(Entity entity, float partialTicks) {
        float red = 0.01F * ModConfig.highlightRed;
        float green = 0.01F * ModConfig.highlightGreen;
        float blue = 0.01F * ModConfig.highlightBlue;
        float alpha = 0.01F * ModConfig.highlightAlpha;

        Minecraft mc = FMLClientHandler.instance().getClient();

        mc.entityRenderer.enableLightmap();
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.enableCull();
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );

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

        int light = entity.getBrightnessForRender();
        int u = light % 65536;
        int v = light / 65536;

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, u, v);
        GlStateManager.color(red, green, blue, alpha);

        mc.getRenderManager().setRenderOutlines(true);
        mc.getRenderManager().renderEntity(entity, renderX, renderY, renderZ, yaw, partialTicks, false);
        mc.getRenderManager().setRenderOutlines(false);

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        mc.entityRenderer.disableLightmap();
    }

    private static void buildCubeLines(BufferBuilder buffer, IBlockAccess world, BlockPos pos,  AxisAlignedBB box) {
        float red = 0.01F * ModConfig.highlightRed;
        float green = 0.01F * ModConfig.highlightGreen;
        float blue = 0.01F * ModConfig.highlightBlue;
        float alpha = 0.01F * ModConfig.highlightAlpha;

        double minX = box.minX, minY = box.minY, minZ = box.minZ;
        double maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;

        int light = world.getCombinedLight(pos.up(), 0);
        int u = light % 65536;
        int v = light / 65536;

        buffer.pos(minX, minY, minZ).color(red, green, blue, 0.0F).lightmap(u, v).endVertex();
        buffer.pos(minX, minY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
        buffer.pos(maxX, minY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
        buffer.pos(maxX, minY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
        buffer.pos(minX, minY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
        buffer.pos(minX, minY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
        buffer.pos(minX, maxY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
        buffer.pos(maxX, maxY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
        buffer.pos(minX, maxY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
        buffer.pos(minX, maxY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
        buffer.pos(minX, maxY, maxZ).color(red, green, blue, 0.0F).lightmap(u, v).endVertex();
        buffer.pos(minX, minY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(red, green, blue, 0.0F).lightmap(u, v).endVertex();
        buffer.pos(maxX, minY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
        buffer.pos(maxX, maxY, minZ).color(red, green, blue, 0.0F).lightmap(u, v).endVertex();
        buffer.pos(maxX, minY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
        buffer.pos(maxX, minY, minZ).color(red, green, blue, 0.0F).lightmap(u, v).endVertex();
    }

    private static void buildFaceOverlay(BufferBuilder buffer, IBlockAccess world, BlockPos pos, AxisAlignedBB box, EnumFacing face) {
        float red = 0.01F * ModConfig.highlightRed;
        float green = 0.01F * ModConfig.highlightGreen;
        float blue = 0.01F * ModConfig.highlightBlue;
        float alpha = 0.01F * ModConfig.highlightAlpha;

        double minX = box.minX, minY = box.minY, minZ = box.minZ;
        double maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;

        int light = world.getCombinedLight(pos.up(), 0);
        int u = light % 65536;
        int v = light / 65536;

        switch (face) {
            case DOWN:
                buffer.pos(maxX, minY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(maxX, minY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(minX, minY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(minX, minY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                break;
            case UP:
                buffer.pos(minX, maxY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(minX, maxY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(maxX, maxY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                break;
            case NORTH:
                buffer.pos(minX, minY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(minX, maxY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(maxX, maxY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(maxX, minY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                break;
            case SOUTH:
                buffer.pos(maxX, minY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(minX, maxY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(minX, minY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                break;
            case WEST:
                buffer.pos(minX, minY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(minX, maxY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(minX, maxY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(minX, minY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                break;
            case EAST:
                buffer.pos(maxX, minY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(maxX, maxY, minZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                buffer.pos(maxX, minY, maxZ).color(red, green, blue, alpha).lightmap(u, v).endVertex();
                break;
        }
    }

    private enum RenderMode {
        lines(GL11.GL_LINE_STRIP, (buffer, state, world, pos, face, boxes) -> {
            GlStateManager.glLineWidth(2.0F);
            for (AxisAlignedBB box : boxes) {
                buildCubeLines(buffer, world, pos, box.grow(0.002D));
            }
        }),

        overlay_all(GL11.GL_QUADS, (buffer, state, world, pos, face, boxes) -> {
            for (EnumFacing side : EnumFacing.VALUES) {
                if (state.shouldSideBeRendered(world, pos, side)) {
                    for (AxisAlignedBB box : boxes) {
                        buildFaceOverlay(buffer, world, pos, box.grow(0.002D), side);
                    }
                }
            }
        }),

        overlay_face(GL11.GL_QUADS, (buffer, state, world, pos, face, boxes) -> {
            for (AxisAlignedBB box : boxes) {
                buildFaceOverlay(buffer, world, pos, box.grow(0.002D), face);
            }
        });

        private final int glMode;
        private final BufferConsumer builder;

        RenderMode(int glMode, BufferConsumer builder) {
            this.glMode = glMode;
            this.builder = builder;
        }
    }

    private interface BufferConsumer {
        void accept(BufferBuilder buffer, IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face, List<AxisAlignedBB> boxes);
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

        @Config.Name("render_mode")
        @Config.Comment("How should the highlight be rendered?")
        public static RenderMode renderMode = RenderMode.overlay_face;

        @Config.Name("highlight_entities")
        @Config.Comment("Should entities be highlighted on mouse-over?")
        public static boolean highlightEntities = true;
    }

}
