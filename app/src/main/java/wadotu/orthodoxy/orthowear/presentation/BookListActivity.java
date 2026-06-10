package wadotu.orthodoxy.orthowear.presentation;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Collections;
import java.util.List;

import wadotu.orthodoxy.orthowear.R;

public class BookListActivity extends AppCompatActivity {

    BibleRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_list);

        repo = new BibleRepository(this);

        ListView listView = findViewById(R.id.bookList);

        List<String> books = repo.getBooks();

// 기존
// Collections.sort(books, (a, b) -> {
//     int ia = BookNameMapper.getCanonicalIndex(a);
//     int ib = BookNameMapper.getCanonicalIndex(b);
//     return Integer.compare(ia, ib);
// });

// 변경된 형태: display name(한국어)도 처리
        Collections.sort(books, (a, b) -> {
            int ia = BookNameMapper.getCanonicalIndexFromDisplay(a);
            int ib = BookNameMapper.getCanonicalIndexFromDisplay(b);
            return Integer.compare(ia, ib);
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                books);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            // String book = repo.getBooks().get(position); // 기존(틀림)
            String book = books.get(position); // 정렬된 리스트에서 가져오기

            Intent i = new Intent(this, ChapterListActivity.class);
            i.putExtra("book", book);
            startActivity(i);
        });
    }
}