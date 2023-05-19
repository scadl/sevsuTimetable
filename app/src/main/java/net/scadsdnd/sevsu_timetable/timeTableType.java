package net.scadsdnd.sevsu_timetable;

public class timeTableType {
    public int groupsString = 3;
    public int dayWidthInCells = 0;
    public int dayHeightInCelss = 7;

    public void loadType(int type){
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
