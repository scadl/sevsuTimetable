package net.scadsdnd.sevsu_timetable;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory;
import org.apache.xmlbeans.StringEnumAbstractBase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 10000;
    private int currentPage = 0;
    private Context curContext;
    private String fPathDir = "";
    private String fPathFile = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner spinWeek = (Spinner) findViewById(R.id.spnWeek);
        spinWeek.setEnabled(false);
        Spinner spinGrp  = (Spinner) findViewById(R.id.spnGroup);
        spinGrp.setEnabled(false);
        Spinner spingDay = (Spinner) findViewById(R.id.spnDay);
        spingDay.setEnabled(false);

        fPathDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        fPathFile = "ionmo_mag1.xlsx";

        Button btnDownload = (Button) findViewById(R.id.btnUpdateFile);
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {



                int writePermCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if(writePermCheck != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
                } else {
                    downloadFreshTT();
                }


            }
        });

        curContext = this;

        loadWeeks();

    }

    private void downloadFreshTT(){
        DownloadManager downloadManager;
        File fXLSX = new File(fPathDir + "/" + fPathFile);
        if (fXLSX.exists()) {
            fXLSX.delete();
        }

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse("https://www.sevsu.ru/univers/shedule/download.php?file=7NEXmnGjONs4M1V8RZudWQ%3D%3D");
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fPathFile);
        Long reference = downloadManager.enqueue(request);

        registerReceiver(downloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {


        switch (requestCode) {
            case REQUEST_WRITE_EXTERNAL_STORAGE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //TODO
                    downloadFreshTT();
                }
                break;

            default:
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


    }

    private BroadcastReceiver downloadComplete = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            loadWeeks();
        }
    };

    private XSSFWorkbook loadExcel(){

        try (XSSFWorkbook wb = XSSFWorkbookFactory.createWorkbook(OPCPackage.create(fPathDir+"/"+fPathFile))) {
            return wb;
        } catch (Exception e) {
            Log.e("TAG", "onCreate error: " + e.getLocalizedMessage());
            Toast.makeText(this, "Error opening file: "+e.getLocalizedMessage(), Toast.LENGTH_LONG);

            try(OPCPackage wb = OPCPackage.open(fPathDir+"/"+fPathFile)){
                return new XSSFWorkbook(wb);
            } catch (Exception e2){
                Log.e("TAG", "onCreate 2 error: " + e2.getLocalizedMessage());
                Toast.makeText(this, "Error opening file 2: "+e2.getLocalizedMessage(), Toast.LENGTH_LONG);
            }
        }

        return null;
    }

    private void loadWeeks(){

        XSSFWorkbook wb = loadExcel();
        if (wb!=null) {

            // https://poi.apache.org/apidocs/5.0/
            // String text = "";
            // XSSFSheet txSh = wb.getSheet("неделя 9(уч.н.27)");
            // XSSFRow txRw = txSh.getRow(3);
            // XSSFCell txCl = txRw.getCell(21); // Группа : c
            // text = txCl.toString();

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
            for (int iS = 0; iS < numSheets; iS++) {
                wkData.add(wb.getSheetName(iS));
            }

            SpinerPopultor(R.id.spnWeek, wkData, 0);
        }

    }

    private void SpinerPopultor(@IdRes int id, List<String> data, int level){

        ArrayAdapter<String> wkAdapt = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, data);
        wkAdapt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spCurSpin = (Spinner) findViewById(id);
        spCurSpin.setAdapter(wkAdapt);

        spCurSpin.setEnabled(true);

        spCurSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (level) {
                    case 0:
                        LoadSecondLevel(i);
                        currentPage = i;
                    break;
                    case 1:
                        LoadFinal(i);
                    break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    List<Short> gpPos = new ArrayList<>();

    private void LoadSecondLevel(int i){

        XSSFWorkbook wb = loadExcel();
        if (wb!=null) {

            XSSFSheet txSh = wb.getSheetAt(i);
            XSSFRow txRw = txSh.getRow(3);
            if (txRw != null) {

                int dataColsNum = txRw.getLastCellNum();
                List<String> gpData = new ArrayList<>();

                for (int iC = 0; iC < dataColsNum; iC++) {
                    if (txRw.getCell(iC).toString() != "") {
                        gpData.add(txRw.getCell(iC).getStringCellValue());
                        gpPos.add((short) iC);
                    }
                }

                SpinerPopultor(R.id.spnGroup, gpData, 1);

                //Log.i("tag", String.valueOf(txRw.getLastCellNum()));

            }
        }

    }

    private void LoadFinal(int indGp){

        Log.i("tag", "Grop arr id: " + String.valueOf(indGp));

        loadDayData(indGp, 0);

        Spinner spingDay = (Spinner) findViewById(R.id.spnDay);
        spingDay.setEnabled(true);

        spingDay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                loadDayData(indGp, i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void loadDayData(int indGp, int indDay){
        XSSFWorkbook wb = loadExcel();
        if (wb!=null) {

            LinearLayout ttTable = (LinearLayout) findViewById(R.id.timeTable);
            ttTable.removeAllViews();

            XSSFSheet txSh = wb.getSheetAt(currentPage);

            for(int iStr=0; iStr<=7; iStr++) {

                int rowDayStart = 8*(indDay);
                XSSFRow txRw = txSh.getRow(6+rowDayStart + iStr);

                LinearLayout tr = new LinearLayout(curContext);
                tr.setGravity(Gravity.TOP);
                tr.setOrientation(LinearLayout.HORIZONTAL);
                tr.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                ));
                tr.setGravity(Gravity.CENTER_HORIZONTAL);

                TextView[] tbItem = new TextView[8];
                boolean lessNotEmpty = false;
                int paddings = 7;

                for(int jCol = 2; jCol<7; jCol++) {

                    XSSFCell txCl = txRw.getCell(gpPos.get(indGp) + jCol);

                    tbItem[iStr] = new TextView(curContext);
                    LinearLayout.LayoutParams lyParam = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    String ouText = "";
                    switch (jCol) {
                        case 4:
                            // Leeson name and Tutor
                            lyParam.weight = 0.7f;
                            tbItem[iStr].setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                            String[] txLessData = txCl.getStringCellValue().split(",");
                            if (txLessData.length>=2) {
                                String[] txTut = txLessData[1].split("\\(");
                                ouText = "<b>"+txLessData[0] + "</b><br>" + txTut[0];
                                lessNotEmpty = true;
                            }
                            Log.d("TAG", "loadDayData: "+txLessData.length);
                            break;
                        case 2:
                            // Lesson number
                            ouText = "<i>" + String.valueOf(Math.round(txCl.getNumericCellValue())) + "</i>";
                            break;
                        default:
                            ouText = txCl.toString();
                            break;
                    }



                    tbItem[iStr].setText(Html.fromHtml(ouText, Html.FROM_HTML_MODE_COMPACT));

                    tbItem[iStr].setLayoutParams(lyParam);
                    tbItem[iStr].setPadding(paddings,paddings,paddings,paddings);

                    tr.addView(tbItem[iStr]);
                }

                if (iStr % 2 == 0) {
                    tr.setBackgroundColor(Color.argb(50, 51, 181, 229));
                } else {
                    tr.setBackgroundColor(Color.argb(50, 175, 210, 223));
                }

                if(lessNotEmpty) {
                    ttTable.addView(tr);
                }
            }
        }
    }
}