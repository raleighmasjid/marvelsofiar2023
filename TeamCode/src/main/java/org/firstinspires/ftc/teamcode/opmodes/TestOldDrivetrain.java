package org.firstinspires.ftc.teamcode.opmodes;

import static com.arcrobotics.ftclib.hardware.motors.Motor.GoBILDA.RPM_312;
import static org.firstinspires.ftc.teamcode.opmodes.MainAuton.gamepadEx1;
import static org.firstinspires.ftc.teamcode.roadrunner.DriveConstants.LOGO_FACING_DIR;
import static org.firstinspires.ftc.teamcode.roadrunner.DriveConstants.USB_FACING_DIR;

import com.arcrobotics.ftclib.drivebase.MecanumDrive;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.hardware.motors.MotorEx;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.subsystems.utilities.ThreadedIMU;

@TeleOp(group = "21836 B")
public final class TestOldDrivetrain extends LinearOpMode {

    @Override
    public void runOpMode() throws InterruptedException {

        // Initialize gamepads:
        gamepadEx1 = new GamepadEx(gamepad1);
        ThreadedIMU imu = new ThreadedIMU(hardwareMap, "imu", new RevHubOrientationOnRobot(LOGO_FACING_DIR, USB_FACING_DIR));
        MecanumDrive drivetrain = new MecanumDrive(
                new MotorEx(hardwareMap, "left front", RPM_312),
                new MotorEx(hardwareMap, "right front", RPM_312),
                new MotorEx(hardwareMap, "left back", RPM_312),
                new MotorEx(hardwareMap, "right back", RPM_312)
        );

        // Control loop:
        while (opModeIsActive()) {
            gamepadEx1.readButtons();

            drivetrain.driveFieldCentric(
                    gamepadEx1.getLeftX(),
                    gamepadEx1.getLeftY(),
                    gamepadEx1.getRightX(),
                    imu.getHeading()
            );
        }
    }
}
