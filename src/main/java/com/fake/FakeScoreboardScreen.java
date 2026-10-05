package com.fake;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Edit screen for the fake scoreboard and the item price labels. Saves when closed. */
public class FakeScoreboardScreen extends Screen {
    private TextFieldWidget nameField;
    private TextFieldWidget moneyField;
    private TextFieldWidget elytraField;
    private TextFieldWidget ingotField;

    public FakeScoreboardScreen() {
        super(Text.literal("Edit fake stuff"));
    }

    private TextFieldWidget field(int cx, int y, String value) {
        TextFieldWidget f = new TextFieldWidget(this.textRenderer, cx - 100, y, 200, 20, Text.literal(""));
        f.setMaxLength(32);
        f.setText(value);
        this.addDrawableChild(f);
        return f;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 85;

        nameField = field(cx, y, FakeConfig.scoreboardName());
        moneyField = field(cx, y + 35, FakeConfig.scoreboardMoney());
        elytraField = field(cx, y + 70, FakeConfig.elytraLabel());
        ingotField = field(cx, y + 105, FakeConfig.ingotLabel());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> this.close())
                .dimensions(cx - 50, y + 135, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int y = this.height / 2 - 85;
        ctx.drawText(this.textRenderer, "Scoreboard name", cx - 100, y - 11, 0xFFFFFFFF, true);
        ctx.drawText(this.textRenderer, "Scoreboard money (after the $)", cx - 100, y + 24, 0xFFFFFFFF, true);
        ctx.drawText(this.textRenderer, "Elytra price label (blank = auto)", cx - 100, y + 59, 0xFFFFFFFF, true);
        ctx.drawText(this.textRenderer, "Netherite ingot price label (blank = auto)", cx - 100, y + 94, 0xFFFFFFFF, true);
    }

    @Override
    public void removed() {
        if (nameField != null) {
            FakeConfig.setScoreboard(nameField.getText(), moneyField.getText());
            FakeConfig.setLabels(elytraField.getText(), ingotField.getText());
        }
        super.removed();
    }
}
