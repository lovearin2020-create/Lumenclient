package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;

/** Dropped items tumble while falling and lie flat on the ground instead of hovering and spinning. */
public class ItemPhysics extends Module {
    public static ItemPhysics INSTANCE;
    private final Setting.Num speed = num("Tumble Speed", 1.0, 0.2, 3.0, 0.1);

    public ItemPhysics() {
        super("Item Physics", "Dropped items lie flat on the ground.", Category.VISUAL);
        INSTANCE = this;
    }

    public void transform(EntityItem item, double x, double y, double z, float pt, IBakedModel model, int count) {
        boolean block = model.isGui3d();
        float sy = model.getItemCameraTransforms().getTransform(ItemCameraTransforms.TransformType.GROUND).scale.y;
        float yaw = (item.getEntityId() * 47 % 360) + item.hoverStart * 20f;
        float spin = item.onGround ? 0 : ((item.getAge() + pt) * 14f * speed.f() + item.hoverStart * 57f) % 360f;
        if (block) {
            GlStateManager.translate((float) x, (float) y + 0.13f, (float) z);
            GlStateManager.rotate(yaw, 0, 1, 0);
            if (!item.onGround) GlStateManager.rotate(spin, 1, 0, 0.4f);
        } else {
            GlStateManager.translate((float) x, (float) y + (item.onGround ? 0.02f : 0.18f), (float) z);
            GlStateManager.rotate(yaw, 0, 1, 0);
            GlStateManager.rotate(-90f + spin, 1, 0, 0);
        }
        GlStateManager.color(1, 1, 1, 1);
    }
}
