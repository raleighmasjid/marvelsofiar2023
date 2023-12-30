package org.firstinspires.ftc.teamcode.subsystems.centerstage;

import static org.firstinspires.ftc.teamcode.opmodes.MainAuton.mTelemetry;
import static org.firstinspires.ftc.teamcode.subsystems.utilities.LEDIndicator.State.OFF;
import static org.firstinspires.ftc.teamcode.subsystems.utilities.LEDIndicator.State.RED;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.subsystems.drivetrains.AutoTurnMecanum;
import org.firstinspires.ftc.teamcode.subsystems.utilities.BulkReader;
import org.firstinspires.ftc.teamcode.subsystems.utilities.LEDIndicator;

@Config
public final class Robot {

    public static double
            maxVoltage = 13,
            TIME_TRAJECTORY_GEN = 0;

    public static boolean isRed = true, isRight = true;

    public final AutoTurnMecanum drivetrain;
    public final Intake intake;
    public final Deposit deposit;

    private final BulkReader bulkReader;
    private final LEDIndicator[] indicators;

    private boolean autoDriveStarted = true;
    private final ElapsedTime autoTimer = new ElapsedTime();

    public Robot(HardwareMap hardwareMap) {
        bulkReader = new BulkReader(hardwareMap);

        drivetrain = new AutoTurnMecanum(hardwareMap);
        drivetrain.update();
        intake = new Intake(hardwareMap);
        deposit = new Deposit(hardwareMap);

        indicators = new LEDIndicator[]{
                new LEDIndicator(hardwareMap, "led left green", "led left red"),
                new LEDIndicator(hardwareMap, "led right green", "led right red")
        };
    }

    private final ElapsedTime i2cTimer = new ElapsedTime();

    public void readSensors() {
        bulkReader.bulkRead();
        i2cTimer.reset();
        drivetrain.imu.update();
        intake.topSensor.update();
        intake.bottomSensor.update();
        mTelemetry.addData("I2C read time", i2cTimer.seconds());
    }

    public void startAutoDrive() {
        autoDriveStarted = false;
        autoTimer.reset();
    }

    public boolean beginUpdatingRunner() {
        if (!autoDriveStarted && autoTimer.seconds() >= TIME_TRAJECTORY_GEN) {
            autoDriveStarted = true;
            return true;
        } else return false;
    }

    public void run() {
        if (intake.pixelsTransferred()) {
            deposit.paintbrush.lockPixels(intake.getColors());
//            scanner.generateTrajectory(deposit.paintbrush.getColors());
        }

        deposit.run();
        intake.run(deposit.paintbrush.getPixelsLocked(), deposit.isRetracted());

        for (LEDIndicator indicator : indicators) indicator.setState(
                drivetrain.isBusy() ? RED :
                OFF
        );
    }

    public void interrupt() {
    }

    public void printTelemetry() {
        drivetrain.printTelemetry(mTelemetry);
        mTelemetry.addLine();
        deposit.paintbrush.printTelemetry();
        mTelemetry.addLine();
        deposit.lift.printTelemetry();
        mTelemetry.addLine();
        intake.printTelemetry();
        mTelemetry.addLine();
        mTelemetry.addLine();
        mTelemetry.addLine();
        drivetrain.printNumericalTelemetry();
        mTelemetry.addLine();
        deposit.lift.printNumericalTelemetry();
        mTelemetry.addLine();
        intake.printNumericalTelemetry();
    }
}
