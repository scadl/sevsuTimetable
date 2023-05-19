package net.scadsdnd.sevsu_timetable;

public class timeTableType {
    public int groupsString;
    public int dayWidthInCells;
    public int dayHeightInCelss;

    public void loadType(int type){
        groupsString = 3;
        dayHeightInCelss = 7;
        switch (type) {
            case 1:
                dayWidthInCells = 7;
                break;
            case 2:
                dayWidthInCells = 12;
                break;
            case 3:
                dayWidthInCells = 9;
                break;
        }
    }
}
