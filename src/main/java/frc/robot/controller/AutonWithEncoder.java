package frc.robot.controller;

import java.util.ArrayList;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.interfaces.Action;
import frc.robot.interfaces.AutonomousController;
import frc.robot.interfaces.DriveController;
import frc.robot.interfaces.Action.ActionType;

public class AutonWithEncoder implements AutonomousController {
    ArrayList<Action> actionMap = new ArrayList<Action>(25);
    DriveController driveController;
    Action prevAction = null;
    int curOp = 0;
    Double startLtPos, startRtPos;
    double autonMaxSpeed = 0.25;
    double encoderToDistanceConversion = 2.355;
    long actionStartTime = 0;
    double startAngle;
    final String DIST_FACTOR = "Inches Per Unit";
    final String MAX_SPEED = "Auton Max Speed";
    final String CUR_ACTION  = "Auton Cur Action";

    public AutonWithEncoder(DriveController driveController) {
        this.driveController = driveController;
        SmartDashboard.putNumber(MAX_SPEED, autonMaxSpeed);
        SmartDashboard.putString(CUR_ACTION, "");
        SmartDashboard.putNumber(DIST_FACTOR, encoderToDistanceConversion);
    }

    @Override
    public void autonomousInit(String[] autoOp) {
        curOp = 0;
        prevAction = null;
        actionMap.clear();
        //System.out.println("Received " + autoOp.length + " autoOp items");
        startAngle = driveController.getYaw();
        autonMaxSpeed = SmartDashboard.getNumber(MAX_SPEED, autonMaxSpeed);
        encoderToDistanceConversion = SmartDashboard.getNumber(DIST_FACTOR, encoderToDistanceConversion);
        actionStartTime = 0;
        startLtPos = null;
        startRtPos = null;
        for (int i = 0; i < autoOp.length; i++) {
            //System.out.println("Processing:" + autoOpr);
            Action p = new Action(autoOp[i].trim());
            if (p.speed==null) p.speed = autonMaxSpeed;
            System.out.println("Adding:" + p);
            actionMap.add(p);
        }
    }

    private double convertToDistance(double encodeLtDelta, double encoderRtDelta) {
        return encoderToDistanceConversion * ((encodeLtDelta+encoderRtDelta)/2.0);
    }

    private double distanceRemainingToSpeed(double remaining, double maxSpeed) {
        maxSpeed = Math.abs(maxSpeed);
        double absRemaining = Math.abs(remaining);
        if (absRemaining<2) return 0;
        boolean reverse = remaining<0? true: false;
        double brakeThrehshold = 6+(maxSpeed-.25)*8;
        double speed = absRemaining<brakeThrehshold ? .25+(maxSpeed-0.25)*absRemaining/brakeThrehshold : maxSpeed;
        return (reverse?-speed:speed);
    }

    private double angleRemainingToSpeed(double remaining) {
        double absRemaining = Math.abs(remaining);
        if (absRemaining<2) return 0;
        boolean reverse = remaining>0? false: true; //positive speed turns clockwise
        double speed = absRemaining<5 ? .25+(autonMaxSpeed-0.25)*absRemaining/5.0 : autonMaxSpeed;
        return (reverse?-speed:speed);
    }

    @Override
    public void actionComplete(Action action) {
        System.out.println("Completed action:"+action);
        startLtPos = startRtPos = null;
        actionStartTime = 0;
        prevAction = action;
        curOp++;
    }

    private double getAngleTurned() {
        double angleTurned = driveController.getYaw()-startAngle;
        return angleTurned;
    }

    @Override
    public Action getNextAction(long timeInAutonomous) {
        Action chosenAction = null;
        double remaining = 0;
        while(curOp<actionMap.size()) {
            if (startLtPos==null) startLtPos =  driveController.getLeftEncoderPosition();
            if (startRtPos==null) startRtPos =  driveController.getRightEncoderPosition();
            chosenAction = actionMap.get(curOp);
            /*if (prevAction==null || !prevAction.equals(chosenAction)) {
                System.out.println("Current action:"+chosenAction);
                prevAction = chosenAction;
            }*/
            if (actionStartTime==0) actionStartTime = timeInAutonomous;
            double currentLtPos = driveController.getLeftEncoderPosition();
            double currentRtPos = driveController.getRightEncoderPosition();
            if (chosenAction.type == ActionType.Move) {
                double distMoved = convertToDistance(currentLtPos-startLtPos, currentRtPos-startRtPos);
                remaining = chosenAction.magnitude-distMoved;
                System.out.println("Distance Moved:"+distMoved+". Remaining:"+remaining);
                chosenAction.speed = distanceRemainingToSpeed(remaining, chosenAction.speed);
                if (chosenAction.speed==0) chosenAction.speed = null; // Trigger to move to next action
                break;
            } else if ((chosenAction.type == ActionType.Cruise) || (chosenAction.type == ActionType.Station)) {
                remaining = chosenAction.magnitude*1000-(timeInAutonomous-actionStartTime);
                if (remaining<=10)
                    chosenAction.speed = null;
                break;
            } else if (chosenAction.type == ActionType.Hold) {
                remaining = chosenAction.magnitude*1000-(timeInAutonomous-actionStartTime);
                System.out.println(chosenAction.type+".. time:"+chosenAction.magnitude*1000+". Remaining:"+remaining);
                if (remaining<=10)
                    chosenAction.speed = null;
                else {
                    double distMoved = convertToDistance(currentLtPos-startLtPos, currentRtPos-startRtPos);
                    remaining = -distMoved;
                    System.out.println("Distance shifted:"+distMoved);
                    chosenAction.speed = distanceRemainingToSpeed(remaining, chosenAction.speed);
                }
                break;
            } else if (chosenAction.type == ActionType.Turn) {
                //double angleTurned = convertToAngle(currentLtPos-startLtPos, currentRtPos-startRtPos);
                double angleTurned = getAngleTurned();
                remaining = chosenAction.magnitude-angleTurned;
                System.out.println("Angle Turned:"+angleTurned+". Remaining:"+remaining);
                chosenAction.speed = angleRemainingToSpeed(remaining);
                if (chosenAction.speed==0) chosenAction.speed = null;
                break;
            } else if (chosenAction.type == ActionType.PCone || chosenAction.type== ActionType.PCube || chosenAction.type== ActionType.SArm ) {
                break;
            } else if (chosenAction.type == ActionType.RCone || chosenAction.type== ActionType.RCube || chosenAction.type== ActionType.GCone || chosenAction.type== ActionType.GCube ) {
                remaining = chosenAction.magnitude*1000-(timeInAutonomous-actionStartTime);
                System.out.println(chosenAction.type+".. time:"+chosenAction.magnitude*1000+". Remaining:"+remaining);
                if (remaining<=10)
                    chosenAction.speed = null;
                else
                    chosenAction.speed = 1.0;
                break;
            } else if (chosenAction.type == ActionType.Stop) {
                chosenAction.speed = null;
                break;
            } else {
                System.out.println("Ignoring "+chosenAction);
                chosenAction.speed = null;
                actionComplete(chosenAction);
            }
        }
        if (chosenAction!=null)
            SmartDashboard.putString(CUR_ACTION, chosenAction.type+" "+remaining+"/"+chosenAction.magnitude);
        else
            SmartDashboard.putString(CUR_ACTION, "None");
        return chosenAction;
    }

    @Override
    public void simulationPeriodic(long tickCount) {
        
    }

    @Override
    public Action calibrate(int calibrationCount, long timeInTest) {
        return null;
    }

    @Override
    public void calibrationInit(int calibrationCycle) {
    }    
}
