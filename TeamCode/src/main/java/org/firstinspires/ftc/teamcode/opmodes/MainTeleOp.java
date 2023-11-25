package org.firstinspires.ftc.teamcode.opmodes;

import static com.arcrobotics.ftclib.gamepad.GamepadKeys.Button.B;
import static com.arcrobotics.ftclib.gamepad.GamepadKeys.Button.DPAD_DOWN;
import static com.arcrobotics.ftclib.gamepad.GamepadKeys.Button.DPAD_LEFT;
import static com.arcrobotics.ftclib.gamepad.GamepadKeys.Button.DPAD_RIGHT;
import static com.arcrobotics.ftclib.gamepad.GamepadKeys.Button.DPAD_UP;
import static com.arcrobotics.ftclib.gamepad.GamepadKeys.Button.RIGHT_BUMPER;
import static com.arcrobotics.ftclib.gamepad.GamepadKeys.Button.X;
import static com.arcrobotics.ftclib.gamepad.GamepadKeys.Trigger.LEFT_TRIGGER;
import static com.arcrobotics.ftclib.gamepad.GamepadKeys.Trigger.RIGHT_TRIGGER;
import static org.firstinspires.ftc.teamcode.opmodes.MainAuton.red;
import static org.firstinspires.ftc.teamcode.opmodes.MainAuton.robot;
import static java.lang.Math.PI;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.subsystems.Robot;

import java.util.List;

@TeleOp(group = "21836 Main")
public class MainTeleOp extends LinearOpMode {

    // Declare objects:
    MultipleTelemetry myTelemetry;
    List<LynxModule> hubs;
    GamepadEx Gamepad1, Gamepad2;

    @Override
    public void runOpMode() throws InterruptedException {

        // Initialize multiple telemetry outputs:
        myTelemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        // Initialize internal hub representations:
        // Switch hubs to manually reset sensor inputs when we tell it to:
        hubs = hardwareMap.getAll(LynxModule.class);
        for (LynxModule hub : hubs) hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);

        // Initialize robot if not already initialized:
        if (robot == null) robot = new Robot(hardwareMap);

        // Initialize gamepads
        Gamepad1 = new GamepadEx(gamepad1);
        Gamepad2 = new GamepadEx(gamepad2);

        // Get gamepad 1 button input and save "lockSlowMode" and "red" booleans for teleop configuration:
        boolean lockSlowMode = false;
        while (opModeInInit()) {
            Gamepad1.readButtons();
            if (Gamepad1.wasJustPressed(RIGHT_BUMPER)) lockSlowMode = !lockSlowMode;
            if (Gamepad1.wasJustPressed(B)) red = true;
            if (Gamepad1.wasJustPressed(X)) red = false;
            myTelemetry.addLine((lockSlowMode ? "SLOW" : "NORMAL") + " mode");
            myTelemetry.addLine((red ? "RED" : "BLUE") + " alliance");
            myTelemetry.update();
        }
        robot.start();

        // Control loop:
        while (opModeIsActive()) {
            // Manually clear old sensor data from the last loop:
            for (LynxModule hub : hubs) hub.clearBulkCache();
            // Read sensors + gamepads:
            Gamepad1.readButtons();
            Gamepad2.readButtons();
            robot.readSensors();

            // Reset current heading as per these keybinds:
            if (Gamepad1.wasJustPressed(DPAD_UP)) robot.drivetrain.setCurrentHeading(0);
            if (Gamepad1.wasJustPressed(DPAD_LEFT)) robot.drivetrain.setCurrentHeading(PI/2);
            if (Gamepad1.wasJustPressed(DPAD_DOWN)) robot.drivetrain.setCurrentHeading(PI);
            if (Gamepad1.wasJustPressed(DPAD_RIGHT)) robot.drivetrain.setCurrentHeading(-PI/2);

            if (lockSlowMode && Gamepad1.isDown(RIGHT_BUMPER)) lockSlowMode = false;
            // Field-centric drive dt with control stick inputs:
            robot.drivetrain.run(
                    Gamepad1.getLeftX(),
                    Gamepad1.getLeftY(),
                    Gamepad1.getRightX(),
                    lockSlowMode || Gamepad1.isDown(RIGHT_BUMPER) ? 1 : 0
            );
            robot.intake.run(Gamepad1.getTrigger(RIGHT_TRIGGER) - Gamepad1.getTrigger(LEFT_TRIGGER));
            robot.deposit.run();
            robot.lift.run(Gamepad1.getRightY());

            // Push telemetry data to multiple outputs (set earlier):
            robot.printTelemetry(myTelemetry);
            myTelemetry.addLine();
            myTelemetry.addLine();
            robot.printNumericalTelemetry(myTelemetry);
            myTelemetry.update();
        }
        robot.interrupt();
    }
}
