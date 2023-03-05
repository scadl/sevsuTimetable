package net.scadsdnd.sevsu_timetable;

public class timeTableType {
    public int groupsString = 0;
    public int dayWidthInCells = 0;
    public int dayHeightInCelss = 0;

    public void loadType(int type){
        switch (type) {
            case 1:
                groupsString = 3;
                dayHeightInCelss = 7;
                dayWidthInCells = 7;
                break;
        }
    }
}
