package wadotu.orthodoxy.orthowear.presentation;

import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.InputDevice;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import wadotu.orthodoxy.orthowear.R;

public class Icon extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.icon);

        recyclerView = findViewById(R.id.imageList);
        LinearSnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        int spacingPx = (int) (5 * getResources().getDisplayMetrics().density);
        recyclerView.addItemDecoration(new SpacingItemDecoration(spacingPx));

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        );

        recyclerView.setAdapter(new ImageAdapter(loadImages()));
    }

    private List<Integer> loadImages() {
        List<Integer> list = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            int resId = getResources()
                    .getIdentifier("orthodoxappicons_" + i, "drawable", getPackageName());
            if (resId != 0) list.add(resId);
        }
        return list;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_SCROLL
                && event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {

            float delta = -event.getAxisValue(MotionEvent.AXIS_SCROLL);
            recyclerView.scrollBy(0, (int) (delta * 80));

            if (vibrator != null) {
                vibrator.vibrate(
                        VibrationEffect.createPredefined(
                                VibrationEffect.EFFECT_TICK
                        )
                );
            }
            return true;
        }
        return super.onGenericMotionEvent(event);
    }
}