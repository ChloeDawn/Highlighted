package net.insomniakitten.highlighted;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.fml.client.FMLClientHandler;
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

    @SubscribeEvent
    public static void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        if (event.getTarget() != null && event.getTarget().typeOfHit == Type.BLOCK) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = FMLClientHandler.instance().getClient();
        if (mc.objectMouseOver == null) return;
        if (mc.objectMouseOver.typeOfHit != Type.BLOCK) return;
        renderBoxes(mc.player, mc.objectMouseOver.getBlockPos(), event.getPartialTicks());
    }

    private static void renderBoxes(EntityPlayer player, BlockPos pos, float partialTicks) {
        AxisAlignedBB eBox = player.getEntityBoundingBox().grow(6.0D);
        World world = player.world;
        IBlockState state = world.getBlockState(pos);
        List<AxisAlignedBB> boxes = new ArrayList<>();

        state = state.getActualState(world, pos);
        state.addCollisionBoxToList(world, pos, eBox, boxes, player, true);
        if (boxes.isEmpty()) boxes.add(state.getSelectedBoundingBox(world, pos));

        double offsetX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double offsetY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double offsetZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
                SourceFactor.ONE, DestFactor.ZERO
        );
        GlStateManager.enableCull();
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        buffer.setTranslation(-offsetX, -offsetY, -offsetZ);

        for (AxisAlignedBB box : boxes) {
            buildCube(buffer, box.grow(0.002D));
        }

        buffer.setTranslation(0.0D, 0.0D, 0.0D);
        tessellator.draw();

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
    }

    private static void buildCube(BufferBuilder buffer, AxisAlignedBB box) {
        float red = 0.01F * ModConfig.highlightRed;
        float green = 0.01F * ModConfig.highlightGreen;
        float blue = 0.01F * ModConfig.highlightBlue;
        float alpha = 0.01F * ModConfig.highlightAlpha;

        double minX = box.minX, minY = box.minY, minZ = box.minZ;
        double maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;

        buffer.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
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
    }

}
