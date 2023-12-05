package de.fanta.casestats.mixin;

import de.fanta.casestats.screens.CaseStatsScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class MixinPauseScreen extends Screen {

    protected MixinPauseScreen(Text title) {
        super(title);
    }

    @Inject(at = @At("TAIL"), method = "initWidgets")
    private void addCustomButton(CallbackInfo ci) {
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("CaseStats"), button -> client.setScreen(new CaseStatsScreen(this)))
                .dimensions(this.width / 2 - 100 + 205, this.height / 4 + 82, 100, 20).build()
        );
    }

}
