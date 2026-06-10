package wadotu.orthodoxy.orthowear.presentation;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import java.util.Arrays;

import wadotu.orthodoxy.orthowear.R;

public class KomboskiniActivity extends AppCompatActivity {

    private FrameLayout container;
    private TextView counterView;
    private Button crossButton, loadButton, saveButton;
    private Spinner beadSpinner;

    private ImageView[] circles;
    private int beadCount = 33;       // 현재 원 개수
    private int currentIndex = 0;     // 다음으로 애니메이션 할 인덱스 (0..beadCount-1)
    private int prayerCount = 0;      // 누적 기도 횟수 (n 표기용)
    private int redCount = 0;         // 현재 빨갛게 된 원 개수 (0..beadCount)

    private SharedPreferences prefs;

    private final String PREF_KEY_PRAY = "saved_prayers";
    private final String PREF_KEY_BEAD = "bead_count";
    private final String PREF_KEY_INDEX = "current_index";
    private final String PREF_KEY_REDCNT = "red_count";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_komboskini);

        prefs = getSharedPreferences("komboskini_data", MODE_PRIVATE);

        container = findViewById(R.id.circleContainer);
        counterView = findViewById(R.id.counterView);
        crossButton = findViewById(R.id.crossButton);
        loadButton = findViewById(R.id.loadButton);
        saveButton = findViewById(R.id.saveButton);
        beadSpinner = findViewById(R.id.beadCountSpinner);

        // 스피너 초기화
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.bead_counts, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        beadSpinner.setAdapter(adapter);

        // 저장된 개수 불러오기
        beadCount = prefs.getInt(PREF_KEY_BEAD, 33);
        // 초기 선택 반영
        int selPos = beadCount == 33 ? 0 : beadCount == 50 ? 1 : 2;
        beadSpinner.setSelection(selPos);

        // 컨테이너가 레이아웃되면 구슬 생성
        container.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                container.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                loadStateAndCreateBeads();
            }
        });

        beadSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int[] options = {33, 50, 100};
                beadCount = options[position];
                prefs.edit().putInt(PREF_KEY_BEAD, beadCount).apply();

                // 초기 인덱스와 빨간 개수 재설정(사용자 원하면 유지하게 변경 가능)
                currentIndex = 0;
                redCount = 0;
                prayerCount = 0;
                updateCounter();
                createBeads();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 저장 버튼
        saveButton.setOnClickListener(v -> {
            prefs.edit()
                    .putInt(PREF_KEY_PRAY, prayerCount)
                    .putInt(PREF_KEY_INDEX, currentIndex)
                    .putInt(PREF_KEY_REDCNT, redCount)
                    .putInt(PREF_KEY_BEAD, beadCount)
                    .apply();

            Toast.makeText(this, "저장 완료!", Toast.LENGTH_SHORT).show();
        });

        // 불러오기 버튼
        loadButton.setOnClickListener(v -> {
            loadStateAndCreateBeads();
            Toast.makeText(this, "불러오기 완료!", Toast.LENGTH_SHORT).show();
        });

        // ☦ 버튼: 진동 + 애니메이션 + 카운트 증가
        crossButton.setOnClickListener(v -> {
            // 햅틱 (우선 performHapticFeedback 빠른 클릭감)
            try {
                // Prefer VibratorManager if available for finer control
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                    if (vm != null) {
                        Vibrator vib = vm.getDefaultVibrator();
                        if (vib != null && vib.hasVibrator()) {
                            VibrationEffect effect = VibrationEffect.createOneShot(20, 80); // 20ms, 강도80
                            vib.vibrate(effect);
                        }
                    } else {
                        crossButton.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                } else {
                    // Older devices
                    Vibrator vi = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (vi != null && vi.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vi.vibrate(VibrationEffect.createOneShot(20, 80));
                        } else {
                            vi.vibrate(20);
                        }
                    } else {
                        crossButton.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                }
            } catch (Exception e) {
                crossButton.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }

            // 안전 검사
            if (circles == null || circles.length == 0) return;

            // animate target bead
            ImageView target = circles[currentIndex % beadCount];
            animateCircle(target, () -> {
                // 애니메이션 끝난 뒤에 빨강으로 확정
                redCount = Math.min(redCount + 1, beadCount);
            });

            // 기도 횟수 즉시 증가
            prayerCount++;
            updateCounter();

            // 다음 인덱스로 (시계 반대 방향 원하면 -1 로 변경)
            currentIndex = (currentIndex + 1) % beadCount;
        });
    }

    private void updateCounter() {
        counterView.setText(prayerCount + "");
    }

    // 로드 상태와 비드 생성
    private void loadStateAndCreateBeads() {
        prayerCount = prefs.getInt(PREF_KEY_PRAY, 0);
        beadCount = prefs.getInt(PREF_KEY_BEAD, 33);
        currentIndex = prefs.getInt(PREF_KEY_INDEX, 0);
        redCount = prefs.getInt(PREF_KEY_REDCNT, 0);

        updateCounter();
        createBeads();
    }

    // createBeads() 수정
    private void createBeads() {
        container.removeAllViews();
        circles = new ImageView[beadCount];

        int cw = container.getWidth();
        int ch = container.getHeight();
        int centerX = cw / 2;
        int centerY = ch / 2 - dpToPx(5);
        int radius = Math.min(cw, ch) / 2 - dpToPx(15);
        if (radius < dpToPx(60)) radius = dpToPx(60);

        int sizeDp;
        if (beadCount == 33) sizeDp = 15;
        else if (beadCount == 50) sizeDp = 11;
        else if (beadCount == 100) sizeDp = 5;
        else sizeDp = 12;

        int sizePx = dpToPx(sizeDp);

        for (int i = 0; i < beadCount; i++) {
            ImageView iv = new ImageView(this);
            iv.setId(View.generateViewId());
            iv.setLayoutParams(new FrameLayout.LayoutParams(sizePx, sizePx));

            Drawable bg = ContextCompat.getDrawable(this, R.drawable.circle_shape);
            Drawable wrap = DrawableCompat.wrap(bg.mutate());

            @ColorInt int idle = ContextCompat.getColor(this, R.color.komboskini_idle);
            DrawableCompat.setTint(wrap, idle);
            iv.setBackground(wrap);

            double step = 360.0 / beadCount;
            double angleDeg = 90 - (i * step);
            double angleRad = Math.toRadians(angleDeg);
            int left = centerX + (int) (radius * Math.cos(angleRad)) - sizePx / 2;
            int top = centerY - (int) (radius * Math.sin(angleRad)) - sizePx / 2;

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) iv.getLayoutParams();
            lp.leftMargin = left;
            lp.topMargin = top;
            container.addView(iv, lp);

            circles[i] = iv;
        }
    }

//        // 빨간색으로 채워야할 bead들 표시 (0..redCount-1)
//        @ColorInt int red = ContextCompat.getColor(this, R.color.komboskini_red);
//        for (int i = 0; i < Math.min(redCount, beadCount); i++) {
//            Drawable d = DrawableCompat.wrap(ContextCompat.getDrawable(this, R.drawable.circle_shape).mutate());
//            DrawableCompat.setTint(d, red);
//            circles[i].setBackground(d);
//        }
//    }

    private void animateCircle(@NonNull ImageView circle, @Nullable Runnable onEnd) {
        @ColorInt int red = ContextCompat.getColor(this, R.color.komboskini_red);
        @ColorInt int ivory = ContextCompat.getColor(this, R.color.komboskini_idle);

        // 🔴 1) 즉시 빨간색으로 변경
        Drawable dRed = DrawableCompat.wrap(ContextCompat.getDrawable(this, R.drawable.circle_shape).mutate());
        DrawableCompat.setTint(dRed, red);
        circle.setBackground(dRed);

        // 2) 2초 후 다시 아이보리색으로 복귀
        circle.postDelayed(() -> {
            Drawable dIvory = DrawableCompat.wrap(ContextCompat.getDrawable(this, R.drawable.circle_shape).mutate());
            DrawableCompat.setTint(dIvory, ivory);
            circle.setBackground(dIvory);

            if (onEnd != null) onEnd.run();
            // ❌ redCount 증가 부분 제거
        }, 2000);
    }

    // 색상 보간 (ARGB)
    private int blendColors(int from, int to, float ratio) {
        final float inv = 1f - ratio;
        int a = (int) (((from >> 24) & 0xff) * inv + ((to >> 24) & 0xff) * ratio);
        int r = (int) (((from >> 16) & 0xff) * inv + ((to >> 16) & 0xff) * ratio);
        int g = (int) (((from >> 8) & 0xff) * inv + ((to >> 8) & 0xff) * ratio);
        int b = (int) (((from >> 0) & 0xff) * inv + ((to >> 0) & 0xff) * ratio);
        return (a & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 자동 저장 (안전) — ❌ redCount 저장 제거
        prefs.edit()
                .putInt(PREF_KEY_PRAY, prayerCount)
                .putInt(PREF_KEY_INDEX, currentIndex)
                .putInt(PREF_KEY_BEAD, beadCount)
                .apply();
    }
}

