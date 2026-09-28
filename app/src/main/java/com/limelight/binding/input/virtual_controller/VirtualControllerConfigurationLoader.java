/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;

import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.nvstream.input.KeyboardPacket;
import com.limelight.preferences.PreferenceConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

public class VirtualControllerConfigurationLoader {
    public static final String OSC_PREFERENCE = "OSC";

    private static int getPercent(
            int percent,
            int total) {
        return (int) (((float) total / (float) 100) * (float) percent);
    }

    // The default controls are specified using a grid of 128*72 cells at 16:9
    private static int screenScale(int units, int height) {
        return (int) (((float) height / (float) 72) * (float) units);
    }

    private static DigitalPad createDigitalPad(
            final VirtualController controller,
            final Context context) {

        DigitalPad digitalPad = new DigitalPad(controller, context);
        digitalPad.addDigitalPadListener(new DigitalPad.DigitalPadListener() {
            @Override
            public void onDirectionChange(int direction) {
                VirtualController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();

                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_LEFT) != 0) {
                    inputContext.inputMap |= ControllerPacket.LEFT_FLAG;
                }
                else {
                    inputContext.inputMap &= ~ControllerPacket.LEFT_FLAG;
                }
                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_RIGHT) != 0) {
                    inputContext.inputMap |= ControllerPacket.RIGHT_FLAG;
                }
                else {
                    inputContext.inputMap &= ~ControllerPacket.RIGHT_FLAG;
                }
                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_UP) != 0) {
                    inputContext.inputMap |= ControllerPacket.UP_FLAG;
                }
                else {
                    inputContext.inputMap &= ~ControllerPacket.UP_FLAG;
                }
                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_DOWN) != 0) {
                    inputContext.inputMap |= ControllerPacket.DOWN_FLAG;
                }
                else {
                    inputContext.inputMap &= ~ControllerPacket.DOWN_FLAG;
                }

                controller.sendControllerInputContext();
            }
        });

        return digitalPad;
    }

    private static DigitalButton createDigitalButton(
            final int elementId,
            final int keyShort,
            final int keyLong,
            final int layer,
            final String text,
            final int icon,
            final VirtualController controller,
            final Context context) {
        DigitalButton button = new DigitalButton(controller, elementId, layer, context);
        button.setText(text);
        button.setIcon(icon);

        button.addDigitalButtonListener(new DigitalButton.DigitalButtonListener() {
            @Override
            public void onClick() {
                VirtualController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();
                inputContext.inputMap |= keyShort;

                controller.sendControllerInputContext();
            }

            @Override
            public void onLongClick() {
                VirtualController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();
                inputContext.inputMap |= keyLong;

                controller.sendControllerInputContext();
            }

            @Override
            public void onRelease() {
                VirtualController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();
                inputContext.inputMap &= ~keyShort;
                inputContext.inputMap &= ~keyLong;

                controller.sendControllerInputContext();
            }
        });

        return button;
    }

    private static DigitalButton createLeftTrigger(
            final int layer,
            final String text,
            final int icon,
            final VirtualController controller,
            final Context context,
            boolean toggleMode) {
        LeftTrigger button = new LeftTrigger(controller, layer, context);
        button.setText(text);
        button.setIcon(icon);
        button.setToggleMode(toggleMode);
        button.setPressed(controller.getControllerInputContext().leftTrigger != 0);
        return button;
    }

    private static DigitalButton createRightTrigger(
            final int layer,
            final String text,
            final int icon,
            final VirtualController controller,
            final Context context) {
        RightTrigger button = new RightTrigger(controller, layer, context);
        button.setText(text);
        button.setIcon(icon);
        button.setPressed(controller.getControllerInputContext().rightTrigger != 0);
        return button;
    }

    private static AnalogStick createLeftStick(
            final VirtualController controller,
            final Context context,
            boolean holdYAxis) {
        return createLeftStick(controller, context, holdYAxis, 30);
    }

    private static AnalogStick createLeftStick(
            final VirtualController controller,
            final Context context,
            boolean holdYAxis,
            int deadZonePercent) {
        return createLeftStick(controller, context, holdYAxis, deadZonePercent,
                VirtualControllerElement.EID_LS);
    }

    private static AnalogStick createLeftStick(
            final VirtualController controller,
            final Context context,
            boolean holdYAxis,
            int deadZonePercent,
            int elementId) {
        return createLeftStick(controller, context, holdYAxis, deadZonePercent, elementId, false);
    }

    private static AnalogStick createLeftStick(
            final VirtualController controller,
            final Context context,
            boolean holdYAxis,
            int deadZonePercent,
            int elementId,
            boolean mapToRightStick) {
        LeftAnalogStick stick = new LeftAnalogStick(controller, context, elementId, mapToRightStick);
        stick.setHoldYAxis(holdYAxis);
        stick.setDeadZonePercent(deadZonePercent);
        VirtualController.ControllerInputContext input = controller.getControllerInputContext();
        stick.setInputPosition(
                (mapToRightStick ? -input.rightStickY : input.leftStickX) / (float) 0x7FFE,
                (mapToRightStick ? input.rightStickX : input.leftStickY) / (float) 0x7FFE);
        return stick;
    }

    private static AnalogStick createRightStick(
            final VirtualController controller,
            final Context context) {
        return createRightStick(controller, context, 30);
    }

    private static AnalogStick createRightStick(
            final VirtualController controller,
            final Context context,
            int deadZonePercent) {
        return createRightStick(controller, context, deadZonePercent,
                VirtualControllerElement.EID_RS);
    }

    private static AnalogStick createRightStick(
            final VirtualController controller,
            final Context context,
            int deadZonePercent,
            int elementId) {
        return createRightStick(controller, context, deadZonePercent, elementId, false);
    }

    private static AnalogStick createRightStick(
            final VirtualController controller,
            final Context context,
            int deadZonePercent,
            int elementId,
            boolean mapToLeftStick) {
        RightAnalogStick stick = new RightAnalogStick(
                controller, context, elementId, mapToLeftStick);
        stick.setDeadZonePercent(deadZonePercent);
        VirtualController.ControllerInputContext input = controller.getControllerInputContext();
        if (mapToLeftStick) {
            stick.setInputPosition(input.leftStickX / (float) 0x7FFE,
                    input.leftStickY / (float) 0x7FFE);
        } else {
            stick.setInputPosition(input.rightStickX / (float) 0x7FFE,
                    input.rightStickY / (float) 0x7FFE);
        }
        return stick;
    }


    private static final int TRIGGER_L_BASE_X = 1;
    private static final int TRIGGER_R_BASE_X = 92;
    private static final int TRIGGER_DISTANCE = 23;
    private static final int TRIGGER_BASE_Y = 31;
    private static final int TRIGGER_WIDTH = 12;
    private static final int TRIGGER_HEIGHT = 9;

    // Face buttons are defined based on the Y button (button number 9)
    private static final int BUTTON_BASE_X = 106;
    private static final int BUTTON_BASE_Y = 1;
    private static final int BUTTON_SIZE = 10;

    private static final int DPAD_BASE_X = 4;
    private static final int DPAD_BASE_Y = 41;
    private static final int DPAD_SIZE = 30;

    private static final int ANALOG_L_BASE_X = 6;
    private static final int ANALOG_L_BASE_Y = 4;
    private static final int ANALOG_R_BASE_X = 98;
    private static final int ANALOG_R_BASE_Y = 42;
    private static final int ANALOG_SIZE = 26;

    private static final int L3_R3_BASE_Y = 60;

    private static final int START_X = 83;
    private static final int BACK_X = 34;
    private static final int START_BACK_Y = 64;
    private static final int START_BACK_WIDTH = 12;
    private static final int START_BACK_HEIGHT = 7;

    // Make the Guide Menu be in the center of START and BACK menu
    private static final int GUIDE_X = START_X-BACK_X;
    private static final int GUIDE_Y = START_BACK_Y;

    // Flight sim layout constants
    private static final int FS_BUTTONS_BASE_Y = 60;
    private static final int FS_BUTTON_WIDTH = 12;
    private static final int FS_BUTTON_HEIGHT = 9;
    private static final int FS_BUTTON_SPACING = 17; // 12 + 5 gap

    public static void createDefaultLayout(final VirtualController controller, final Context context) {

        DisplayMetrics screen = context.getResources().getDisplayMetrics();
        PreferenceConfiguration config = PreferenceConfiguration.readPreferences(context);

        // Displace controls on the right by this amount of pixels to account for different aspect ratios
        int rightDisplacement = screen.widthPixels - screen.heightPixels * 16 / 9;

        int height = screen.heightPixels;

        if (config.oscLayout.equals("flight_sim")) {
            createFlightSimLayout(controller, context, config, height, rightDisplacement);
            return;
        }

        // NOTE: Some of these getPercent() expressions seem like they can be combined
        // into a single call. Due to floating point rounding, this isn't actually possible.

        if (!config.onlyL3R3)
        {
            controller.addElement(createDigitalPad(controller, context),
                    screenScale(DPAD_BASE_X, height),
                    screenScale(DPAD_BASE_Y, height),
                    screenScale(DPAD_SIZE, height),
                    screenScale(DPAD_SIZE, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_A,
                    !config.flipFaceButtons ? ControllerPacket.A_FLAG : ControllerPacket.B_FLAG, 0, 1,
                    !config.flipFaceButtons ? "A" : "B", -1, controller, context),
                    screenScale(BUTTON_BASE_X, height) + rightDisplacement,
                    screenScale(BUTTON_BASE_Y + 2 * BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_B,
                    config.flipFaceButtons ? ControllerPacket.A_FLAG : ControllerPacket.B_FLAG, 0, 1,
                    config.flipFaceButtons ? "A" : "B", -1, controller, context),
                    screenScale(BUTTON_BASE_X + BUTTON_SIZE, height) + rightDisplacement,
                    screenScale(BUTTON_BASE_Y + BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_X,
                    !config.flipFaceButtons ? ControllerPacket.X_FLAG : ControllerPacket.Y_FLAG, 0, 1,
                    !config.flipFaceButtons ? "X" : "Y", -1, controller, context),
                    screenScale(BUTTON_BASE_X - BUTTON_SIZE, height) + rightDisplacement,
                    screenScale(BUTTON_BASE_Y + BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_Y,
                    config.flipFaceButtons ? ControllerPacket.X_FLAG : ControllerPacket.Y_FLAG, 0, 1,
                    config.flipFaceButtons ? "X" : "Y", -1, controller, context),
                    screenScale(BUTTON_BASE_X, height) + rightDisplacement,
                    screenScale(BUTTON_BASE_Y, height),
                    screenScale(BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height)
            );

            controller.addElement(createLeftTrigger(
                    1, "LT", -1, controller, context, config.ltToggle),
                    screenScale(TRIGGER_L_BASE_X, height),
                    screenScale(TRIGGER_BASE_Y, height),
                    screenScale(TRIGGER_WIDTH, height),
                    screenScale(TRIGGER_HEIGHT, height)
            );

            controller.addElement(createRightTrigger(
                    1, "RT", -1, controller, context),
                    screenScale(TRIGGER_R_BASE_X + TRIGGER_DISTANCE, height) + rightDisplacement,
                    screenScale(TRIGGER_BASE_Y, height),
                    screenScale(TRIGGER_WIDTH, height),
                    screenScale(TRIGGER_HEIGHT, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_LB,
                    ControllerPacket.LB_FLAG, 0, 1, "LB", -1, controller, context),
                    screenScale(TRIGGER_L_BASE_X + TRIGGER_DISTANCE, height),
                    screenScale(TRIGGER_BASE_Y, height),
                    screenScale(TRIGGER_WIDTH, height),
                    screenScale(TRIGGER_HEIGHT, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_RB,
                    ControllerPacket.RB_FLAG, 0, 1, "RB", -1, controller, context),
                    screenScale(TRIGGER_R_BASE_X, height) + rightDisplacement,
                    screenScale(TRIGGER_BASE_Y, height),
                    screenScale(TRIGGER_WIDTH, height),
                    screenScale(TRIGGER_HEIGHT, height)
            );

            controller.addElement(createLeftStick(controller, context, config.leftStickHoldY),
                    screenScale(ANALOG_L_BASE_X, height),
                    screenScale(ANALOG_L_BASE_Y, height),
                    screenScale(ANALOG_SIZE, height),
                    screenScale(ANALOG_SIZE, height)
            );

            controller.addElement(createRightStick(controller, context),
                    screenScale(ANALOG_R_BASE_X, height) + rightDisplacement,
                    screenScale(ANALOG_R_BASE_Y, height),
                    screenScale(ANALOG_SIZE, height),
                    screenScale(ANALOG_SIZE, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_BACK,
                    ControllerPacket.BACK_FLAG, 0, 2, "BACK", -1, controller, context),
                    screenScale(BACK_X, height),
                    screenScale(START_BACK_Y, height),
                    screenScale(START_BACK_WIDTH, height),
                    screenScale(START_BACK_HEIGHT, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_START,
                    ControllerPacket.PLAY_FLAG, 0, 3, "START", -1, controller, context),
                    screenScale(START_X, height) + rightDisplacement,
                    screenScale(START_BACK_Y, height),
                    screenScale(START_BACK_WIDTH, height),
                    screenScale(START_BACK_HEIGHT, height)
            );
        }
        else {
            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_LSB,
                    ControllerPacket.LS_CLK_FLAG, 0, 1, "L3", -1, controller, context),
                    screenScale(TRIGGER_L_BASE_X, height),
                    screenScale(L3_R3_BASE_Y, height),
                    screenScale(TRIGGER_WIDTH, height),
                    screenScale(TRIGGER_HEIGHT, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_RSB,
                    ControllerPacket.RS_CLK_FLAG, 0, 1, "R3", -1, controller, context),
                    screenScale(TRIGGER_R_BASE_X + TRIGGER_DISTANCE, height) + rightDisplacement,
                    screenScale(L3_R3_BASE_Y, height),
                    screenScale(TRIGGER_WIDTH, height),
                    screenScale(TRIGGER_HEIGHT, height)
            );
        }

        if(config.showGuideButton){
            controller.addElement(createDigitalButton(VirtualControllerElement.EID_GDB,
                            ControllerPacket.SPECIAL_BUTTON_FLAG, 0, 1, "GUIDE", -1, controller, context),
                    screenScale(GUIDE_X, height)+ rightDisplacement,
                    screenScale(GUIDE_Y, height),
                    screenScale(START_BACK_WIDTH, height),
                    screenScale(START_BACK_HEIGHT, height)
            );
        }

        controller.setOpacity(config.oscOpacity);
    }

    private static void createFlightSimLayout(
            final VirtualController controller,
            final Context context,
            final PreferenceConfiguration config,
            int height,
            int rightDisplacement) {

        // Throttle hold is shared with the standard layout and remains user-configurable.
        controller.addElement(createLeftStick(controller, context, config.leftStickHoldY, 0,
                        VirtualControllerElement.EID_FS_LS, true),
                screenScale(ANALOG_L_BASE_X, height),
                screenScale(ANALOG_R_BASE_Y, height),
                screenScale(ANALOG_SIZE, height),
                screenScale(ANALOG_SIZE, height)
        );

        // Both flight sticks use zero deadzone.
        controller.addElement(createRightStick(controller, context, 0,
                        VirtualControllerElement.EID_FS_RS, true),
                screenScale(ANALOG_R_BASE_X, height) + rightDisplacement,
                screenScale(ANALOG_R_BASE_Y, height),
                screenScale(ANALOG_SIZE, height),
                screenScale(ANALOG_SIZE, height)
        );

        // 4 buttons in a horizontal row at the bottom
        int buttonY = screenScale(FS_BUTTONS_BASE_Y, height);
        int buttonW = screenScale(FS_BUTTON_WIDTH, height);
        int buttonH = screenScale(FS_BUTTON_HEIGHT, height);
        int buttonSpacing = screenScale(FS_BUTTON_SPACING, height);
        int rowWidth = buttonW * 4 + (buttonSpacing - buttonW) * 3;
        int startX = (context.getResources().getDisplayMetrics().widthPixels - rowWidth) / 2;

        // ARM — toggle for leftTrigger
        LeftTrigger armBtn = new LeftTrigger(controller, VirtualControllerElement.EID_FS_ARM, 1, context);
        armBtn.setText("ARM");
        armBtn.setToggleMode(config.ltToggle);
        armBtn.setPressed(controller.getControllerInputContext().leftTrigger != 0);
        controller.addElement(armBtn,
                startX + buttonSpacing * 0,
                buttonY, buttonW, buttonH
        );

        // FLIP — momentary A button
        controller.addElement(createDigitalButton(
                VirtualControllerElement.EID_FS_BRAKE,
                ControllerPacket.A_FLAG, 0, 1, "FLIP", -1, controller, context),
                startX + buttonSpacing * 1,
                buttonY, buttonW, buttonH
        );

        // RESET — momentary B button
        controller.addElement(createDigitalButton(
                VirtualControllerElement.EID_FS_FIRE,
                ControllerPacket.B_FLAG, 0, 1, "RESET", -1, controller, context),
                startX + buttonSpacing * 2,
                buttonY, buttonW, buttonH
        );

        // ESC — sends Escape key to host (for game menu)
        DigitalButton escBtn = new DigitalButton(controller, VirtualControllerElement.EID_FS_SEC, 1, context);
        escBtn.setText("ESC");
        escBtn.addDigitalButtonListener(new DigitalButton.DigitalButtonListener() {
            @Override
            public void onClick() {
                controller.sendKeyboardInput((short) 0x801B, KeyboardPacket.KEY_DOWN, (byte) 0, (byte) 0);
            }

            @Override
            public void onLongClick() {
            }

            @Override
            public void onRelease() {
                controller.sendKeyboardInput((short) 0x801B, KeyboardPacket.KEY_UP, (byte) 0, (byte) 0);
            }
        });
        controller.addElement(escBtn,
                startX + buttonSpacing * 3,
                buttonY, buttonW, buttonH
        );

        controller.setOpacity(config.oscOpacity);
    }

    public static void saveProfile(final VirtualController controller,
                                   final Context context) {
        SharedPreferences.Editor prefEditor = context.getSharedPreferences(OSC_PREFERENCE, Activity.MODE_PRIVATE).edit();

        for (VirtualControllerElement element : controller.getElements()) {
            String prefKey = ""+element.elementId;
            try {
                prefEditor.putString(prefKey, element.getConfiguration().toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        prefEditor.apply();
    }

    public static void loadFromPreferences(final VirtualController controller, final Context context) {
        SharedPreferences pref = context.getSharedPreferences(OSC_PREFERENCE, Activity.MODE_PRIVATE);

        for (VirtualControllerElement element : controller.getElements()) {
            String prefKey = ""+element.elementId;

            String jsonConfig = pref.getString(prefKey, null);
            if (jsonConfig != null) {
                try {
                    element.loadConfiguration(new JSONObject(jsonConfig));
                } catch (JSONException e) {
                    e.printStackTrace();

                    // Remove the corrupt element from the preferences
                    pref.edit().remove(prefKey).apply();
                }
            }
        }
    }
}
