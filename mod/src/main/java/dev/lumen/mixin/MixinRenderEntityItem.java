package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderEntityItem.class)
public abstract class MixinRenderEntityItem {
    @Shadow protected int func_177078_a(ItemStack stack) { return 1; }

    /** Item Physics / 2D Items: replace only the hover-and-spin positioning; vanilla still draws the models. */
    @Inject(method = "func_177077_a", at = @At("HEAD"), cancellable = true)
    private void lumen$position(EntityItem item, double x, double y, double z, float partialTicks, IBakedModel model, CallbackInfoReturnable<Integer> cir) {
        if (item == null || model == null || item.getEntityItem() == null) return;
        int n = Hooks.itemTransform(item, x, y, z, partialTicks, model, this.func_177078_a(item.getEntityItem()));
        if (n >= 0) cir.setReturnValue(n);
    }
}
