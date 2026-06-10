package wadotu.orthodoxy.orthowear.presentation;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import wadotu.orthodoxy.orthowear.R;

public class ChapterListActivity extends AppCompatActivity {

    BibleRepository repo;
    String book;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_list);

        repo = new BibleRepository(this);

        book = getIntent().getStringExtra("book");

        ListView list = findViewById(R.id.chapterList);
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                repo.getChapters(book));

        list.setAdapter(adapter);

        list.setOnItemClickListener((a, v, pos, id) -> {
            int chapter = repo.getChapters(book).get(pos);

            Intent i = new Intent(this, VerseListActivity.class);
            i.putExtra("book", book);
            i.putExtra("chapter", chapter);
            startActivity(i);
        });
    }
}
