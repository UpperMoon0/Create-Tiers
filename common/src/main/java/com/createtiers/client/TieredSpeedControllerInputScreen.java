package com.createtiers.client;

import java.util.function.IntConsumer;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Compact numeric editor for tiered Rotation Speed Controllers.
 *
 * <p>Create's stock value board scales its physical width with the maximum value,
 * which is fine for vanilla's 256 RPM but becomes wider than the screen for high
 * tier limits. Tiered controllers instead use a signed integer field:
 * positive and negative values select the two rotation directions.</p>
 */
public final class TieredSpeedControllerInputScreen extends Screen {

    private final int initialValue;
    private final int maxRpm;
    private final IntConsumer onApply;

    private EditBox input;
    private Component validationMessage = Component.empty();

    public TieredSpeedControllerInputScreen(int initialValue, int maxRpm, IntConsumer onApply) {
        super(Component.translatable("createtiers.speed_controller.input.title"));
        this.initialValue = initialValue;
        this.maxRpm = Math.max(1, maxRpm);
        this.onApply = onApply;
    }

    @Override
    protected void init() {
        int fieldWidth = 150;
        input = new EditBox(
                font,
                (width - fieldWidth) / 2,
                height / 2 - 4,
                fieldWidth,
                20,
                Component.translatable("createtiers.speed_controller.input.field"));
        input.setMaxLength(11);
        input.setFilter(TieredSpeedControllerInputScreen::isPartialSignedInteger);
        input.setValue(Integer.toString(initialValue));
        input.setResponder(ignored -> validate());
        addRenderableWidget(input);
        setInitialFocus(input);
        validate();
    }

    static boolean isPartialSignedInteger(String value) {
        if (value.isEmpty() || "-".equals(value)) {
            return true;
        }
        int start = value.charAt(0) == '-' ? 1 : 0;
        if (start == value.length()) {
            return true;
        }
        for (int i = start; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    static Integer parseSignedRpm(String text, int maxRpm) {
        if (text == null || text.isEmpty() || "-".equals(text)) {
            return null;
        }
        final int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
        long magnitude = Math.abs((long) value);
        return value != 0 && magnitude <= Math.max(1, maxRpm) ? value : null;
    }

    private Integer parsedValue() {
        return input == null ? null : parseSignedRpm(input.getValue(), maxRpm);
    }

    private boolean validate() {
        Integer value = parsedValue();
        if (value == null) {
            String text = input == null ? "" : input.getValue();
            validationMessage = text.isEmpty() || "-".equals(text)
                    ? Component.translatable("createtiers.speed_controller.input.invalid")
                    : Component.translatable("createtiers.speed_controller.input.range", maxRpm);
            return false;
        }
        validationMessage = Component.empty();
        return true;
    }

    private boolean submit() {
        if (!validate()) {
            return true;
        }
        onApply.accept(parsedValue());
        onClose();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            return submit();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xA0101010);

        int centerY = height / 2;
        graphics.drawCenteredString(font, title, width / 2, centerY - 38, 0xFBDC7D);
        graphics.drawCenteredString(
                font,
                Component.translatable("createtiers.speed_controller.input.hint", maxRpm),
                width / 2,
                centerY - 23,
                0xDDDDDD);

        super.render(graphics, mouseX, mouseY, partialTick);

        Component footer = validationMessage.getString().isEmpty()
                ? Component.translatable("createtiers.speed_controller.input.controls")
                : validationMessage;
        int color = validationMessage.getString().isEmpty() ? 0xAAAAAA : 0xFF5555;
        graphics.drawCenteredString(font, footer, width / 2, centerY + 24, color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}