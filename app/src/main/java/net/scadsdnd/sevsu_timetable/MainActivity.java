package net.scadsdnd.sevsu_timetable;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 10000;
    private int currentPage = 0;
    private Context curContext;
    private String fPathDir;
    private String fPathFile;
    private String fDownloadLink;
    private SharedPreferences shPf;
    private timeTableType ttCurrent;
    private int failedDownloads = 0;
    private long ladDownloadId = 0;
    private int curTtType = 0;
    private Date curFdate;
    private SimpleDateFormat simpFormat;

    Uri uriStream;

    EditText txtUrl = null;
    TextView txtStatus = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ttCurrent = new timeTableType();
        ttCurrent.loadType(1);

        curFdate = Calendar.getInstance().getTime();
        simpFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        shPf = MainActivity.this.getPreferences(Context.MODE_PRIVATE);

        curContext = this;
        fPathDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()+"/sevsu_tt/";
        fPathFile = shPf.getString("last_file","");
        fDownloadLink = shPf.getString("download_link", "");
        curTtType = shPf.getInt("tt_type", 1);

        txtUrl = (EditText) findViewById(R.id.edtDownloadLink);
        txtUrl.setText(fDownloadLink);

        txtStatus = (TextView) findViewById(R.id.tvStatus);

        Spinner spinWeek = (Spinner) findViewById(R.id.spnWeek);
        spinWeek.setEnabled(false);
        Spinner spinGrp  = (Spinner) findViewById(R.id.spnGroup);
        spinGrp.setEnabled(false);
        Spinner spingDay = (Spinner) findViewById(R.id.spnDay);
        spingDay.setEnabled(false);

        Spinner spinType = (Spinner) findViewById(R.id.spinType);
        spinType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                SharedPreferences.Editor shPfEdit = shPf.edit();
                shPfEdit.putInt("tt_type",i);
                shPfEdit.commit();

                ttCurrent.loadType(i);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ImageButton btnDownload = (ImageButton) findViewById(R.id.btnUpdateFile);
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                downloadFreshTT();
            }
        });

        ImageButton btnWeb = (ImageButton) findViewById(R.id.btnWebSource);
        btnWeb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = "https://www.sevsu.ru/univers/shedule/";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });

        downloadFreshTT();
    }

    private void downloadFreshTT(){

        try {


            // Open file with user selected app
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent = Intent.createChooser(intent, "Chose File");

            ActivityResultLauncher<Intent> startActivityIntent = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {

                            if (result.getResultCode() == Activity.RESULT_OK){

                                SharedPreferences.Editor shPfEdit = shPf.edit();
                                shPfEdit.putString("download_link", txtUrl.getText().toString());
                                shPfEdit.putString("last_file","ionmo_"+simpFormat.format(curFdate)+".xlsx");
                                shPfEdit.commit();

                                failedDownloads = 0;

                                // ActivityResult 1st getData() - intent, 2nd - file URI
                                uriStream = result.getData().getData();
                                loadWeeks();

                            }
                            Log.w("tag_intent_data", result.toString() + result.getData().toString() );
                        }
                    }
            );

            startActivityIntent.launch(intent);

        } catch (Exception e){
            txtStatus.setText(R.string.link_error);
            Log.e("TAG", "downloadFreshTT: " + "Broken link URL or, "+ e.getLocalizedMessage());
        }


    }

    private XSSFWorkbook loadExcel(){

        // content://com.android.providers.downloads.documents/document/11
        // fPathDir+fPathFile
        try {
            InputStream dataStream = getContentResolver().openInputStream(uriStream);
            try (OPCPackage wb = OPCPackage.open(dataStream)) {

                return new XSSFWorkbook(wb);

            } catch (Exception e) {

                Log.e("TAG!", "onCreate error: " + "Can't open XLSX file. " + e.getLocalizedMessage());
                txtStatus.setText(R.string.link_error);

            }
        } catch (Exception e){
            Log.e("TAG!", "onCreate error: " + "Can't open XLSX file. " + e.getLocalizedMessage());
        }
        return null;
    }

    private void loadWeeks(){
        XSSFWorkbook wb = loadExcel();
        if (wb!=null) {
            int numSheets = wb.getNumberOfSheets();
            List<String> wkData = new ArrayList<>();
            for (int iS = 0; iS < numSheets; iS++) {
                wkData.add(wb.getSheetName(iS));
            }
            SpinerPopultor(R.id.spnWeek, wkData, 0);
        }

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
    }

    private void SpinerPopultor(@IdRes int id, List<String> data, int level){

        LinearLayout ttTable = (LinearLayout) findViewById(R.id.timeTable);
        ttTable.removeAllViews();

        Spinner spDay = (Spinner) findViewById(R.id.spnDay);
        spDay.setSelection(0);

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
            XSSFRow txRw = txSh.getRow(ttCurrent.groupsString);
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

    private LinearLayout ttStringBuild(String[] cellData, int iStr){

        int paddings = 7;

        LinearLayout tr = new LinearLayout(curContext);
        tr.setGravity(Gravity.TOP);
        tr.setOrientation(LinearLayout.HORIZONTAL);
        tr.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        tr.setGravity(Gravity.CENTER_HORIZONTAL);


            TextView[] tbItem = new TextView[8];

            for(int jCol = 2; jCol< ttCurrent.dayWidthInCells; jCol++) {

                tbItem[jCol] = new TextView(curContext);
                LinearLayout.LayoutParams lyParam = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

                switch (jCol) {
                    case 4:
                        // Leeson name and Tutor
                        lyParam.weight = 0.7f;
                        tbItem[jCol].setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        break;
                }

                tbItem[jCol].setText(Html.fromHtml(cellData[jCol], Html.FROM_HTML_MODE_COMPACT));

                tbItem[jCol].setLayoutParams(lyParam);
                tbItem[jCol].setPadding(paddings,paddings,paddings,paddings);

                tr.addView(tbItem[jCol]);
            }

            if (iStr % 2 == 0) {
                tr.setBackgroundColor(Color.argb(50, 51, 181, 229));
            } else {
                if(iStr>=0) {
                    tr.setBackgroundColor(Color.argb(50, 175, 210, 223));
                } else {
                    tr.setBackgroundColor(Color.argb(50, 100, 100, 100));
                }
            }

            return tr;

    }

    private void loadDayData(int indGp, int indDay){
        XSSFWorkbook wb = loadExcel();
        if (wb!=null) {

            LinearLayout ttTable = (LinearLayout) findViewById(R.id.timeTable);
            ttTable.removeAllViews();

            XSSFSheet txSh = wb.getSheetAt(currentPage);

            String[] outHeader = new String[7];
            outHeader[2] = "<i>№</i>";
            outHeader[3] = "<i>Нач.</i>";
            outHeader[4] = "<i>Дисциплина и преподавтель</i>";
            outHeader[5] = "<i>Тип</i>";
            outHeader[6] = "<i>Ауд.</i>";

            ttTable.addView(ttStringBuild(outHeader, -1));

            for(int iStr=0; iStr<=ttCurrent.dayWidthInCells; iStr++) {

                int rowDayStart = (ttCurrent.dayHeightInCelss+1)*indDay;
                XSSFRow txRw = txSh.getRow(ttCurrent.groupsString+3+rowDayStart + iStr);

                boolean lessNotEmpty = false;
                String[] ouText = new String[ttCurrent.dayHeightInCelss+1];

                for(int jCol = 2; jCol< ttCurrent.dayHeightInCelss; jCol++) {

                    XSSFCell txCl = txRw.getCell(gpPos.get(indGp) + jCol);

                    switch (jCol) {
                        case 4:
                            String[] txLessData = txCl.getStringCellValue().split(",");
                            if (txLessData.length>=2) {
                                String[] txTut = txLessData[1].split("\\(");
                                ouText[jCol] = "<b>"+txLessData[0] + "</b><br>" + txTut[0];
                                lessNotEmpty = true;
                            }
                            Log.d("TAG", "loadDayData: "+txLessData.length);
                            break;
                        case 2:
                            // Lesson number
                            ouText[jCol] = "<i><span style='color:blue'>" + String.valueOf(Math.round(txCl.getNumericCellValue())) + "</span></i>";
                            break;
                        default:
                            ouText[jCol] = txCl.toString();
                            break;
                    }
                }

                if(lessNotEmpty) {
                    ttTable.addView(ttStringBuild(ouText, iStr));
                }
            }
        }
    }
}