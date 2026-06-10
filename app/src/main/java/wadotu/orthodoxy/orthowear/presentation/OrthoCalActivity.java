// Copyright 2026. Wadotu Applications
// All rights reserved.
package wadotu.orthodoxy.orthowear.presentation;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

import wadotu.orthodoxy.orthowear.R;

public class OrthoCalActivity extends AppCompatActivity {

    private TextView tvDate, tvFeast, tvFasting, tvTone, tvReading;
    private TextView btnPrev, btnNext;
    private ProgressBar loadingProgress;
    private Calendar currentDisplayDate;
    private int calendarMode = 0; // 0: Revised Julian (gregorian), 1: Julian

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ortho_cal);

        tvDate = findViewById(R.id.tv_date);
        tvFeast = findViewById(R.id.tv_feast);
        tvFasting = findViewById(R.id.tv_fasting);
        tvTone = findViewById(R.id.tv_tone);
        tvReading = findViewById(R.id.tv_reading);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        loadingProgress = findViewById(R.id.loading_progress);

        SharedPreferences prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        calendarMode = prefs.getInt("calendar_mode", 0);

        currentDisplayDate = Calendar.getInstance();

        loadCalendarData();

        btnPrev.setOnClickListener(v -> {
            currentDisplayDate.add(Calendar.DAY_OF_YEAR, -1);
            loadCalendarData();
        });

        btnNext.setOnClickListener(v -> {
            currentDisplayDate.add(Calendar.DAY_OF_YEAR, 1);
            loadCalendarData();
        });
    }

    private void loadCalendarData() {
        loadingProgress.setVisibility(View.VISIBLE);

        String apiMode = (calendarMode == 0) ? "gregorian" : "julian";
        int year = currentDisplayDate.get(Calendar.YEAR);
        int month = currentDisplayDate.get(Calendar.MONTH) + 1;
        int day = currentDisplayDate.get(Calendar.DAY_OF_MONTH);

        OrthoCalendar.getFullCalendarDay(this, apiMode, year, month, day, new OrthoCalendar.FullCalendarCallback() {
            @Override
            public void onDataLoaded(OrthoDay orthoDay) {
                loadingProgress.setVisibility(View.GONE);

                Calendar infoCal = (Calendar) currentDisplayDate.clone();
                // Julian calendar display logic: adjust the date string only, 
                // the API already handles fetching the correct data for the requested calendar.
                if (calendarMode == 1) {
                    infoCal.add(Calendar.DAY_OF_YEAR, -13);
                }

                OrthoCalendar.CalendarInfo info = OrthoCalendar.processDay(OrthoCalActivity.this, orthoDay, calendarMode, infoCal);

                tvDate.setText(info.date);
                tvFeast.setText(info.feast);
                tvFasting.setText(info.fasting);
                tvTone.setText(info.tone);
                tvReading.setText(info.dailyVerse);

                if (info.readingBook != null && !info.readingBook.isEmpty()) {
                    tvReading.setOnClickListener(v -> {
                        Intent intent = new Intent(OrthoCalActivity.this, VerseListActivity.class);
                        intent.putExtra("book", info.readingBook);
                        intent.putExtra("chapter", info.readingChapter);
                        startActivity(intent);
                    });
                    tvReading.setAlpha(1.0f);
                } else {
                    tvReading.setOnClickListener(null);
                    tvReading.setAlpha(0.7f);
                }
            }

            @Override
            public void onError(String message) {
                loadingProgress.setVisibility(View.GONE);
                Toast.makeText(OrthoCalActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}