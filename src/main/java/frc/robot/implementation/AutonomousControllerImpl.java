package frc.robot.implementation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.interfaces.AutonomousController;
import frc.robot.main.Pair;
import frc.robot.main.DashboardItem;

public class AutonomousControllerImpl implements AutonomousController {
    ArrayList<Pair> speedMap = new ArrayList<Pair>(5);
    ArrayList<Pair> rotateMap = new ArrayList<Pair>(5);
    ArrayList<Pair> actionMap = new ArrayList<Pair>(50);
    int calibrationsCount;
    Pair[] calibrateSequence = new Pair[10];
    final int CALIBRATION_TIME = 5000; // in milli sec
    
    @Override
    public void autonomousInit(String[] autoOp) {
        initMagnitudeToPhysicalMap();
        setOperationList(autoOp);
    }
    
    public void setOperationList(String[] autoOp) {
        System.out.println("Received " + autoOp.length + " autoOp items");
        int time = 0;
        for (int i = 0; i < autoOp.length; i++) {
            String op = autoOp[i].trim();
            System.out.println("Processing:" + op);
            int spaceIndx = op.indexOf(' ');
            String action = op.substring(0,spaceIndx);
            Integer distangl = Integer.parseInt(op.substring(spaceIndx+1));
            List<Pair> map = null;
            switch (action) {
                case "Move": 
                    map = speedMap;
                    break;
                case "Turn": 
                    map = rotateMap; 
                    break;
                case "SArm":
                case "GCone":
                case "PCone":
                case "RCone":
                case "PCube":
                case "RCube":
                    time += distangl;
                    Pair p = new Pair(0.0,time * 1000,action);
                    System.out.println("Adding " + p);
                    actionMap.add(p);
                    break;
                default:
                    System.out.println("Unknown operation:"+action);
            }
            if (map!=null) {
                boolean reverse = false;
                if (distangl < 0) {
                    reverse = true;
                    distangl = -distangl;
                }
                for (Pair entPair : map) {
                    // System.out.println("Checking "+entPair);
                    if (entPair.p2 <= distangl) {
                        Integer duration = distangl / entPair.p2;
                        distangl = distangl % entPair.p2;
                        time += duration;
                        Pair p = new Pair(reverse ? -entPair.p1 : entPair.p1, time * 1000, action);
                        actionMap.add(p);
                        System.out.println("Adding " + p + ". Remaining:" + distangl);
                    }
                }
            }
        }
        System.out.println("COMPLETED processing " + autoOp.length + " autoOp items");
    }

    @Override
    public Pair getNextAction(long timeInAutonomous) {
        Pair chosenAction = null;
        Iterator<Pair> itr = actionMap.iterator();
        while (itr.hasNext()) {
            Pair action = itr.next();
            // System.out.println("Checking: "+action);
            if (timeInAutonomous-action.p2 >= -10) {
                System.out.println("Removing completed action:" + action);
                itr.remove();
            } else {
                chosenAction = action;
                break;
            }
        }
        return chosenAction;
    }

    @Override
    public Pair calibrate(int currentCalibration, long timeInCalibration) {
        if (currentCalibration>calibrationsCount) 
            return null;
        else {
            if (timeInCalibration<CALIBRATION_TIME) 
                return calibrateSequence[currentCalibration-1]; 
            else
                return new Pair(null, null, "Stop"); // Nothing more to do for this count
        }
    }
    
    // Update these values after calibration in code or smart dashboard
    void initMagnitudeToPhysicalMap() {
        speedMap.add(new Pair(1.0, (int)SmartDashboard.getNumber(DashboardItem.DistOn_100.getKey(), DashboardItem.DistOn_100.getDefaultValue()))); // Speed value of 1.0 results in 20 inches/sec
        speedMap.add(new Pair(0.5, (int)SmartDashboard.getNumber(DashboardItem.DistOn_50.getKey(), DashboardItem.DistOn_50.getDefaultValue()))); 
        speedMap.add(new Pair(0.25,(int)SmartDashboard.getNumber(DashboardItem.DistOn_25.getKey(), DashboardItem.DistOn_25.getDefaultValue()))); 
        speedMap.add(new Pair(0.05,(int)SmartDashboard.getNumber(DashboardItem.DistOn_10.getKey(), DashboardItem.DistOn_10.getDefaultValue()))); 
        System.out.println("SpeedToDistance Map");
        for (Pair p: speedMap) {
            System.out.println(p);
        }

        rotateMap.add(new Pair(1.0, (int)SmartDashboard.getNumber(DashboardItem.RotaOn_100.getKey(), DashboardItem.RotaOn_100.getDefaultValue())));  // Rotation value of 1.0 results in 45 deg/sec
        rotateMap.add(new Pair(0.5, (int)SmartDashboard.getNumber(DashboardItem.RotaOn_50.getKey(), DashboardItem.RotaOn_50.getDefaultValue()))); 
        rotateMap.add(new Pair(0.25,(int)SmartDashboard.getNumber(DashboardItem.RotaOn_25.getKey(), DashboardItem.RotaOn_25.getDefaultValue()))); 
        rotateMap.add(new Pair(0.05,(int)SmartDashboard.getNumber(DashboardItem.RotaOn_10.getKey(), DashboardItem.DistOn_10.getDefaultValue()))); 
        System.out.println("SpeedToAngle Map");
        for (Pair p: rotateMap) {
            System.out.println(p);
        }
    }

    @Override
    public void calibrationInit(int calibrationCycle) {
        calibrationsCount=0;
        calibrateSequence[calibrationsCount++] = new Pair(1.0, null, "Move"); //Move for 5 sec at max speed
        calibrateSequence[calibrationsCount++] = new Pair(0.5, null, "Move"); //Move for 5 sec at half speed
        calibrateSequence[calibrationsCount++] = new Pair(0.25, null, "Move"); //Move for 5 sec at qtr speed
        calibrateSequence[calibrationsCount++] = new Pair(0.1, null, "Move"); //Move for 5 sec at 10% of max speed

        calibrateSequence[calibrationsCount++] = new Pair(1.0, null, "Turn"); //Turn for 5 sec at max speed
        calibrateSequence[calibrationsCount++] = new Pair(0.5, null, "Turn"); //Turn for 5 sec at half speed      
        calibrateSequence[calibrationsCount++] = new Pair(0.25, null, "Turn"); //Turn for 5 sec at qtr speed      
        calibrateSequence[calibrationsCount++] = new Pair(0.1, null, "Turn"); //Turn for 5 sec at 10% max speed
    }
}