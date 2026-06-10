package wadotu.orthodoxy.orthowear.presentation;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import wadotu.orthodoxy.orthowear.R;

public class VerseListActivity extends AppCompatActivity {

    BibleRepository repo;
    String book;
    int chapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verse_list);

        repo = new BibleRepository(this);

        book = getIntent().getStringExtra("book");
        chapter = getIntent().getIntExtra("chapter", 1);

        ListView listView = findViewById(R.id.verseList);

        ArrayAdapter<Verse> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                        repo.getVerses(book, chapter));

        listView.setAdapter(adapter);
    }
}
