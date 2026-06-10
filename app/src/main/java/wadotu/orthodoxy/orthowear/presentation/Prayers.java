package wadotu.orthodoxy.orthowear.presentation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableRecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import wadotu.orthodoxy.orthowear.R;

public class Prayers extends AppCompatActivity {

    private WearableRecyclerView recyclerView;
    private View contentView;
    private TextView titleView;
    private TextView textView;
    private Button backButton;

    private List<PrayerItem> prayerItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prayers);

        recyclerView = findViewById(R.id.prayer_list);
        contentView = findViewById(R.id.prayer_content_scroll);
        titleView = findViewById(R.id.prayer_title);
        textView = findViewById(R.id.prayer_text);
        backButton = findViewById(R.id.btn_back_to_list);

        loadPrayers();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (contentView.getVisibility() == View.VISIBLE) {
                    contentView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
        recyclerView.setAdapter(new PrayerAdapter());

        backButton.setOnClickListener(v -> {
            contentView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        });
    }

    private void loadPrayers() {
        try {
            InputStream is = getResources().openRawResource(R.raw.prayers_ko);
            InputStreamReader reader = new InputStreamReader(is);
            Type listType = new TypeToken<List<PrayerItem>>() {}.getType();
            prayerItems = new Gson().fromJson(reader, listType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showPrayer(PrayerItem item) {
        titleView.setText(item.title);
        textView.setText(item.content);
        recyclerView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
    }

    private static class PrayerItem {
        int id;
        String title;
        String content;
    }

    private class PrayerAdapter extends RecyclerView.Adapter<PrayerAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prayer, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PrayerItem item = prayerItems.get(position);
            holder.textView.setText(item.title);
            holder.itemView.setOnClickListener(v -> showPrayer(item));
        }

        @Override
        public int getItemCount() {
            return prayerItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(View view) {
                super(view);
                textView = view.findViewById(R.id.tv_prayer_item_title);
            }
        }
    }
}
