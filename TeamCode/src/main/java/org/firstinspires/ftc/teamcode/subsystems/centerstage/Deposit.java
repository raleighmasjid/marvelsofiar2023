package org.firstinspires.ftc.teamcode.subsystems.centerstage;

import static com.arcrobotics.ftclib.hardware.motors.Motor.GoBILDA.RPM_1150;
import static com.arcrobotics.ftclib.hardware.motors.Motor.ZeroPowerBehavior.FLOAT;
import static com.qualcomm.robotcore.util.Range.clip;
import static org.firstinspires.ftc.teamcode.control.vision.pipelines.placementalg.Pixel.Color.EMPTY;
import static org.firstinspires.ftc.teamcode.opmodes.AutonVars.BOTTOM_ROW_HEIGHT;
import static org.firstinspires.ftc.teamcode.opmodes.MainAuton.mTelemetry;
import static org.firstinspires.ftc.teamcode.subsystems.centerstage.Deposit.Lift.ROW_CLIMBING;
import static org.firstinspires.ftc.teamcode.subsystems.centerstage.Deposit.Lift.ROW_RETRACTED;
import static org.firstinspires.ftc.teamcode.subsystems.centerstage.Deposit.Paintbrush.TIME_DROP_SECOND;
import static org.firstinspires.ftc.teamcode.subsystems.centerstage.Deposit.Paintbrush.TIME_FLOOR_RETRACTION;
import static org.firstinspires.ftc.teamcode.subsystems.centerstage.Deposit.Paintbrush.TIME_SCORING_RETRACTION;
import static org.firstinspires.ftc.teamcode.subsystems.centerstage.Robot.maxVoltage;
import static org.firstinspires.ftc.teamcode.subsystems.utilities.SimpleServoPivot.getAxonServo;
import static org.firstinspires.ftc.teamcode.subsystems.utilities.SimpleServoPivot.getGoBildaServo;
import static org.firstinspires.ftc.teamcode.subsystems.utilities.SimpleServoPivot.getReversedServo;

import static java.lang.Math.round;

import com.acmerobotics.dashboard.config.Config;
import com.arcrobotics.ftclib.hardware.motors.MotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.control.controllers.PIDController;
import org.firstinspires.ftc.teamcode.control.gainmatrices.FeedforwardGains;
import org.firstinspires.ftc.teamcode.control.gainmatrices.KalmanGains;
import org.firstinspires.ftc.teamcode.control.gainmatrices.LowPassGains;
import org.firstinspires.ftc.teamcode.control.gainmatrices.PIDGains;
import org.firstinspires.ftc.teamcode.control.gainmatrices.ProfileConstraints;
import org.firstinspires.ftc.teamcode.control.motion.State;
import org.firstinspires.ftc.teamcode.control.vision.pipelines.placementalg.Pixel;
import org.firstinspires.ftc.teamcode.subsystems.utilities.SimpleServoPivot;

@Config
public final class Deposit {

    public final Paintbrush paintbrush;
    public final Lift lift;

    Deposit(HardwareMap hardwareMap) {
        lift = new Lift(hardwareMap);
        paintbrush = new Paintbrush(hardwareMap);
    }

    void run(boolean intakeClear) {

        if (!paintbrush.droppedPixel && (paintbrush.timer.seconds() >= TIME_DROP_SECOND)) {
            paintbrush.droppedPixel = true;
            lift.setTargetRow(ROW_RETRACTED);
        }

        lift.run(intakeClear);

        boolean extendPaintbrush = intakeClear && lift.targetRow != ROW_CLIMBING && lift.isScoring();
        paintbrush.pivot.setActivated(extendPaintbrush);
        paintbrush.run();
    }

    boolean isExtended() {
        return lift.isExtended() || paintbrush.retractionTimer.seconds() <= (paintbrush.floor ? TIME_FLOOR_RETRACTION : TIME_SCORING_RETRACTION);
    }

    @Config
    public static final class Lift {

        public static PIDGains pidGains = new PIDGains(
                0.3,
                0.2,
                0,
                0.2
        );

        public static FeedforwardGains feedforwardGains = new FeedforwardGains(
                0.0015,
                0.000085
        );

        public static LowPassGains lowPassGains = new LowPassGains(
                0.85,
                20
        );

        public static ProfileConstraints profileConstraints = new ProfileConstraints(
                40,
                100,
                0
        );

        public static KalmanGains kalmanGains = new KalmanGains(
                3,
                5,
                10
        );

        public static double
                kG = 0.15,
                INCHES_PER_TICK = 0.0088581424,
                HEIGHT_PIXEL = 2.59945,
                ROW_CLIMBING = 6.25,
                ROW_RETRACTED = -1,
                ROW_FLOOR_SCORING = -0.5,
                HEIGHT_MIN = 0.5,
                PERCENT_OVERSHOOT = 0,
                SPEED_RETRACTION = -0.05,
                POS_1 = 0,
                POS_2 = 25;

        // Motors and variables to manage their readings:
        private final MotorEx[] motors;
        private State currentState, targetState;
        private final PIDController controller = new PIDController();

        private double manualLiftPower, targetRow;

        // Battery voltage sensor and variable to track its readings:
        private final VoltageSensor batteryVoltageSensor;

        private Lift(HardwareMap hardwareMap) {
            this.batteryVoltageSensor = hardwareMap.voltageSensor.iterator().next();
            this.motors = new MotorEx[]{
                    new MotorEx(hardwareMap, "lift right", RPM_1150),
                    new MotorEx(hardwareMap, "lift left", RPM_1150)
            };
            motors[1].setInverted(true);
            for (MotorEx motor : motors) motor.setZeroPowerBehavior(FLOAT);
            reset();
        }

        public void reset() {
            targetRow = ROW_RETRACTED;
            controller.reset();
            for (MotorEx motor : motors) motor.encoder.reset();
            targetState = currentState = new State();
        }

        public boolean isScoring() {
            return targetRow != ROW_RETRACTED;
        }

        public boolean isExtended() {
            return currentState.x > HEIGHT_MIN;
        }

        public void setTargetRow(double targetRow) {
            this.targetRow = clip(targetRow, ROW_RETRACTED, 10);
            double inches = rowToInches(this.targetRow);
            targetState = new State(inches);
        }

        private static double rowToInches(double row) {
            if (row == ROW_RETRACTED) return 0;
            return row * HEIGHT_PIXEL + BOTTOM_ROW_HEIGHT;
        }

        public void changeRow(int deltaRow) {
            setTargetRow(round(targetRow + deltaRow));
        }

        public void setLiftPower(double manualLiftPower) {
            this.manualLiftPower = manualLiftPower;
        }

        public void readSensors() {
            currentState = new State(INCHES_PER_TICK * motors[0].encoder.getPosition());
            controller.setGains(pidGains);
        }

        private void run(boolean intakeClear) {

            if (manualLiftPower != 0) targetState = currentState;

            State setpoint = intakeClear ? targetState : new State(0);
            controller.setTarget(setpoint);

            boolean withinRetractionTolerance = currentState.x <= HEIGHT_MIN;
            boolean tryingToRetract = withinRetractionTolerance && setpoint.x == 0;

            double voltageScalar = maxVoltage / batteryVoltageSensor.getVoltage();
            double output =
                    (
                            withinRetractionTolerance ? 0 : kG * voltageScalar
                    ) + (

                        manualLiftPower != 0 ?      manualLiftPower * voltageScalar :
                        tryingToRetract ?           SPEED_RETRACTION * voltageScalar :
                        controller.calculate(currentState)

                    )
            ;
            for (MotorEx motor : motors) motor.set(output);
        }

        void printTelemetry() {
            String namedPos =
                    targetRow == ROW_RETRACTED ? "Retracted" :
                    targetRow == ROW_CLIMBING ? "Climbing" :
                    "Row " + targetRow;
            mTelemetry.addData("Named target position", namedPos);
        }

        void printNumericalTelemetry() {
            mTelemetry.addData("Current position (in)", currentState.x);
            mTelemetry.addData("Target position (in)", targetState.x);
            mTelemetry.addLine();
            mTelemetry.addData("Lift error derivative (in/s)", controller.getFilteredErrorDerivative());
            mTelemetry.addLine();
            mTelemetry.addData("kD (computed)", pidGains.kD);
        }
    }

    @Config
    public static final class Paintbrush {

        public static double
                ANGLE_PIVOT_OFFSET = 17.25,
                ANGLE_PIVOT_SCORING = 127,
                ANGLE_PIVOT_FLOOR = 190,
                ANGLE_CLAW_OPEN = 13,
                ANGLE_CLAW_CLOSED = 50,
                ANGLE_HOOK_OPEN = 8,
                ANGLE_HOOK_CLOSED = 45,
                TIME_DROP_FIRST = 0.5,
                TIME_DROP_SECOND = 0.65,
                TIME_SCORING_RETRACTION = 0.2,
                TIME_FLOOR_RETRACTION = 0.25;

        private final SimpleServoPivot pivot, hook, claw;

        private final ElapsedTime timer = new ElapsedTime(), retractionTimer = new ElapsedTime();
        private boolean droppedPixel = true, floor = false;
        private int pixelsLocked = 0;
        final Pixel.Color[] colors = {EMPTY, EMPTY};

        private Paintbrush(HardwareMap hardwareMap) {
            pivot = new SimpleServoPivot(
                    ANGLE_PIVOT_OFFSET,
                    ANGLE_PIVOT_OFFSET + ANGLE_PIVOT_SCORING,
                    getAxonServo(hardwareMap, "deposit left"),
                    getReversedServo(getAxonServo(hardwareMap, "deposit right"))
            );

            hook = new SimpleServoPivot(
                    ANGLE_HOOK_OPEN,
                    ANGLE_HOOK_CLOSED,
                    getGoBildaServo(hardwareMap, "pixel hook")
            );

            claw = new SimpleServoPivot(
                    ANGLE_CLAW_OPEN,
                    ANGLE_CLAW_CLOSED,
                    getReversedServo(getGoBildaServo(hardwareMap, "pixel claw"))
            );
        }

        int getPixelsLocked() {
            return pixelsLocked;
        }

        public void lockPixels(Pixel.Color... colors) {
            int pixelsInIntake = 0;
            for (Pixel.Color color : colors) if (color != EMPTY) pixelsInIntake++;

            if (pixelsInIntake == 1) {
                if (pixelsLocked == 0) this.colors[1] = colors[0];
                if (pixelsLocked == 1) this.colors[0] = colors[0];
            } else if (pixelsInIntake == 2) {
                this.colors[1] = colors[1];
                this.colors[0] = colors[0];
            }

            pixelsLocked = clip(pixelsLocked + pixelsInIntake, 0, 2);
        }

        public void toggleFloor() {
            floor = !floor;
        }

        public void dropPixel() {
            pixelsLocked = clip(pixelsLocked - 1, 0, 2);
            if (pixelsLocked <= 1) colors[0] = EMPTY;
            if (pixelsLocked == 0) {
                colors[1] = EMPTY;
                if (pivot.isActivated()) {
                    droppedPixel = false;
                    timer.reset();
                }
            }
        }

        private void run() {
            pivot.updateAngles(
                    ANGLE_PIVOT_OFFSET,
                    ANGLE_PIVOT_OFFSET + (floor ? ANGLE_PIVOT_FLOOR : ANGLE_PIVOT_SCORING)
            );
            claw.updateAngles(ANGLE_CLAW_OPEN, ANGLE_CLAW_CLOSED);
            hook.updateAngles(ANGLE_HOOK_OPEN, ANGLE_HOOK_CLOSED);

            claw.setActivated(pixelsLocked >= 1);
            hook.setActivated(pixelsLocked == 2);

            pivot.run();
            claw.run();
            hook.run();

            if (pivot.isActivated()) retractionTimer.reset();
        }

        void printTelemetry() {
            mTelemetry.addData("First color", colors[0].name());
            mTelemetry.addData("Second color", colors[1].name());
        }
    }
}
