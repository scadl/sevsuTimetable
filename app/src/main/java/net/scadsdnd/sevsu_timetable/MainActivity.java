package net.scadsdnd.sevsu_timetable;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String text = "";

        try (OPCPackage pkg = OPCPackage.open(getResources().openRawResource(R.raw.exams))) {
            XSSFWorkbook wb = new XSSFWorkbook(pkg);
            XSSFExcelExtractor extractor = new XSSFExcelExtractor(wb);
            extractor.setFormulasNotResults(true);
            extractor.setIncludeSheetNames(false);
            text = extractor.getText();
            wb.close();
        } catch (Exception e) {
            Log.e("TAG", "onCreate: " + e.getLocalizedMessage());
        }

        TextView tvOut = (TextView) findViewById(R.id.helloOut);
        tvOut.setText(text);


    }
}