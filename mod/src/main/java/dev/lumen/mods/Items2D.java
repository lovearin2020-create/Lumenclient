package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.MathHelper;

/** Draws every dropped stack as a single sprite facing you: fewer models to render in item-heavy fights. */
public class Items2D extends Module {
    public static Items2D INSTANCE;

    public Items2D() {
        super("2D Items", "Dropped items as single flat sprites (faster).", Category.PERFORMANCE);
        INSTANCE = this;
    }

    public void transform(EntityItem item, double x, double y, double z, float pt, IBakedModel model) {
        float sy = model.getItemCameraTransforms().getTransform(ItemCameraTransforms.TransformType.GROUND).scale.y;
        float bob = MathHelper.sin((item.getAge() + pt) / 10.0F + item.hoverStart) * 0.1F + 0.1F;
        GlStateManager.translate((float) x, (float) y + bob + 0.25F * sy, (float) z);
        if (model.isGui3d()) GlStateManager.rotate(((item.getAge() + pt) / 20.0F + item.hoverStart) * 57.295776F, 0, 1, 0);
        else GlStateManager.rotate(180f - mc.getRenderManager().playerViewY, 0, 1, 0);
        GlStateManager.color(1, 1, 1, 1);
    }
}
