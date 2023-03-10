package frc.robot.main;

public enum DashboardItem {
    Calibrate_Cycle("Calibrate Cycle", 0),
    DistOn_100("DistOn 100", 0), DistOn_50("DistOn 50",20), 
    DistOn_25("DistOn 25",10), DistOn_10("DistOn 10",1), 
    RotaOn_100("RotOn 100",0), RotaOn_50("RotOn 50",30), 
    RotaOn_25("RotOn 25", 15), RotaOn_10("RotOn 10",2);

    private String key;
    private int defaultValue;

    DashboardItem(String key, int defaultValue) {
      this.key = key;
      this.defaultValue = defaultValue;
    }

    public String getKey() { return key;}
    public int getDefaultValue() { return defaultValue; }
  }
