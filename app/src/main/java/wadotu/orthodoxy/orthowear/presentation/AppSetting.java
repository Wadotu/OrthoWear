package wadotu.orthodoxy.orthowear.presentation;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableRecyclerView;

import java.util.Arrays;
import java.util.List;

import wadotu.orthodoxy.orthowear.R;

public class AppSetting extends AppCompatActivity {

    private static final List<String> LANGUAGES = Arrays.asList("ko", "el", "en", "cu");
    private static final List<String> LANGUAGE_NAMES = Arrays.asList("한국어", "Ελληνικά", "English", "Church Slavonic");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        WearableRecyclerView recyclerView = findViewById(R.id.language_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new LanguageAdapter());
    }

    private class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prayer, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textView.setText(LANGUAGE_NAMES.get(position));
            holder.itemView.setOnClickListener(v -> {
                LocaleHelper.setLocale(AppSetting.this, LANGUAGES.get(position));
                
                // Restart MainActivity and finish current stack to apply locale
                Intent intent = new Intent(AppSetting.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }

        @Override
        public int getItemCount() {
            return LANGUAGES.size();
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
