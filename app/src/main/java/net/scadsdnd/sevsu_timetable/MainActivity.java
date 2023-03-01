package net.scadsdnd.sevsu_timetable;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String text = "";

        // https://poi.apache.org/apidocs/5.0/

        try (OPCPackage pkg = OPCPackage.open(getResources().openRawResource(R.raw.ionmo_22_o_m))) {
            XSSFWorkbook wb = new XSSFWorkbook(pkg);

            XSSFSheet txSh = wb.getSheet("неделя 9(уч.н.27)");
            XSSFRow txRw = txSh.getRow(3);
            XSSFCell txCl = txRw.getCell(21); // Группа : c
            text = txCl.toString();

            // Col 21 - 27 = Группа : И/м-22-2-о's props
            // Col 21 - weekday
            // col 22 - date
            // col 23 - lesson number
            // col 24 - start time
            // col 25 - lesson title, tutor, room
            // col 26 - lesson type
            // col 27 - Room number
            // Row 3 - Group name - Группа : И/м-22-2-о
            // Row 6 - 13 - Monday, all lessons
            // Row 14 - 21 - Tuesday, all lessons
            // Row 22 - 29 - Wensday, all lessons
            // Row 30 - 37 - Thusday, all lessons
            // Row 38 - 45 - Friday, all lessons
            // Row 46 - 53 - Saturday, all lessons



            int numSheets = wb.getNumberOfSheets();
            List<String> wkData = new ArrayList<>();
            for(int iS=0; iS<numSheets; iS++) {
                wkData.add(wb.getSheetName(iS));
            }

            ArrayAdapter<String> wkAdapt = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, wkData);
            wkAdapt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            Spinner spWeek = (Spinner) findViewById(R.id.spnWeek);
            spWeek.setAdapter(wkAdapt);

            Context myAct = this;

            spWeek.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    XSSFSheet txSh = wb.getSheetAt(i);
                    XSSFRow txRw = txSh.getRow(3);
                    if(txRw != null) {

                        int dataColsNum = txRw.getLastCellNum();
                         List<String> gpData = new ArrayList<>();

                        for(int iC=0; iC<dataColsNum; iC++){
                            if(txRw.getCell(iC).toString()!=""){
                                gpData.add(txRw.getCell(iC).toString());
                            }
                        }

                        ArrayAdapter<String> gpAdapt = new ArrayAdapter<String>(myAct, android.R.layout.simple_spinner_item, gpData);
                        gpAdapt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                        Spinner spGroup = (Spinner) findViewById(R.id.spnGroup);
                        spGroup.setAdapter(gpAdapt);

                        Log.i("tag", String.valueOf(txRw.getLastCellNum()));

                        spGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> adapterView) {

                            }
                        });

                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            wb.close();

        } catch (Exception e) {
            Log.e("TAG", "onCreate error: " + e.getLocalizedMessage());
            Toast.makeText(this, "Error opening file: "+e.getLocalizedMessage(), Toast.LENGTH_LONG);
        }

    }
}