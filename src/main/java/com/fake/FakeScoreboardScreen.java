package com.fake;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Edit screen for the fake scoreboard. Saves when closed (Esc or Save). */
public class FakeScoreboardScreen extends Screen {
    private TextFieldWidget nameField;
    private TextFieldWidget moneyField;

    public FakeScoreboardScreen() {
        super(Text.literal("Edit scoreboard"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 40;

        nameField = new TextFieldWidget(this.textRenderer, cx - 100, y, 200, 20, Text.literal("Name"));
        nameField.setMaxLength(32);
        nameField.setText(FakeConfig.scoreboardName());
        this.addDrawableChild(nameField);

        moneyField = new TextFieldWidget(this.textRenderer, cx - 100, y + 40, 200, 20, Text.literal("Money"));
        moneyField.setMaxLength(32);
        moneyField.setText(FakeConfig.scoreboardMoney());
        this.addDrawableChild(moneyField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> this.close())
                .dimensions(cx - 50, y + 75, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int y = this.height / 2 - 40;
        ctx.drawText(this.textRenderer, "Name", cx - 100, y - 11, 0xFFFFFFFF, true);
        ctx.drawText(this.textRenderer, "Money (shown after the $)", cx - 100, y + 29, 0xFFFFFFFF, true);
    }

    @Override
    public void removed() {
        if (nameField != null && moneyField != null) {
            FakeConfig.setScoreboard(nameField.getText(), moneyField.getText());
        }
        super.removed();
    }
}
