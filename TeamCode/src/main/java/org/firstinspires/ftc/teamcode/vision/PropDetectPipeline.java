package org.firstinspires.ftc.teamcode.vision;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvPipeline;

public class PropDetectPipeline extends OpenCvPipeline {
    Telemetry telemetry;
    Mat mat = new Mat();
    public enum Location {
        LEFT,
        RIGHT,
        NOT_FOUND
    }
    private Location location;

    static final Rect LEFT_ROI = new Rect(
            new Point(0, 0),
            new Point(320, 480));
    static final Rect RIGHT_ROI = new Rect (
            new Point( 320, 0),
            new Point(640, 480));
    static double PERCENT_COLOR_THRESHOLD = 0.4;

    public PropDetectPipeline(Telemetry t) { telemetry = t; }


    @Override
    public void init(Mat input) {
        // Executed before the first call to processFrame
    }

    @Override
    public Mat processFrame(Mat input) {
        // Executed every time a new frame is dispatched
        Imgproc.cvtColor(input, mat, Imgproc.COLOR_RGB2HSV);
        Scalar lowHSV = new Scalar(56 ,56, 56);//new Scalar(23,50, 70);
        Scalar highHSV = new Scalar(255, 255,255);//new Scalar(32, 255,255);

        Core.inRange(mat, lowHSV, highHSV, mat);

        Mat left = mat.submat(LEFT_ROI);
        Mat right = mat.submat(RIGHT_ROI);

        double leftValue = Core.sumElems(left).val[0] / LEFT_ROI.area() / 255;
        double rightValue = Core.sumElems(right).val[0] / RIGHT_ROI.area() / 255;

        left.release();
        right.release();

//        telemetry.addData("Left raw value", (int) Core.sumElems(left).val[0]);
//        telemetry.addData("Right raw value", (int) Core.sumElems(right).val[0]);
        telemetry.addData("Left percentage", Math.round(leftValue * 100) + "%");
        telemetry.addData("Right percentage", Math.round(rightValue * 100) + "%");

        boolean stoneLeft = leftValue > PERCENT_COLOR_THRESHOLD;
        boolean stoneRight = rightValue > PERCENT_COLOR_THRESHOLD;

        if (stoneLeft && stoneRight) {
            location = Location.NOT_FOUND;
            telemetry.addData("Skystone Location", "not found");
        }
        else if (stoneLeft) {
            location = Location.RIGHT;
            telemetry.addData("Skystone Location", "right");
        }
        else {
            location = Location.LEFT;
            telemetry.addData("Skystone Location", "left");
        }
        telemetry.update();

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2RGB);

        Scalar colorStone = new Scalar(255, 0, 0);
        Scalar colorSkystone = new Scalar(0, 255, 0);

        Imgproc.rectangle(mat, LEFT_ROI, location == Location.LEFT? colorSkystone:colorStone);
        Imgproc.rectangle(mat, RIGHT_ROI, location == Location.RIGHT? colorSkystone:colorStone);

        return mat;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public void onViewportTapped() {
        // Executed when the image display is clicked by the mouse or tapped
        // This method is executed from the UI thread, so be careful to not
        // perform any sort heavy processing here! Your app might hang otherwise
    }

}