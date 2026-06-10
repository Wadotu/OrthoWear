package wadotu.orthodoxy.orthowear.presentation;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import wadotu.orthodoxy.orthowear.R;

public class BibleRepository {

    // book → chapter → verses
    private final Map<String, Map<Integer, List<Verse>>> data = new HashMap<>();

    public BibleRepository(Context context) {
        load(context);
    }

    private void load(Context ctx) {
        try {
            InputStream is = ctx.getResources().openRawResource(R.raw.bible_ko);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = br.readLine()) != null) {

                // example format: "Gen 1:1 한 처음에 하느님께서..."
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(" ", 2);
                if (parts.length < 2) continue;

                String bookAbbr = parts[0].trim();
                String koreaBook = BookNameMapper.get(bookAbbr);

                String[] chapterSplit = parts[1].split(" ", 2);
                if (chapterSplit.length < 1) continue;

                String[] chapVerse = chapterSplit[0].split(":");
                if (chapVerse.length != 2) continue;

                int chapter = Integer.parseInt(chapVerse[0]);
                int verse = Integer.parseInt(chapVerse[1]);

                String text = (chapterSplit.length > 1) ? chapterSplit[1] : "";

                data.putIfAbsent(koreaBook, new HashMap<>());
                data.get(koreaBook).putIfAbsent(chapter, new ArrayList<>());
                data.get(koreaBook).get(chapter).add(new Verse(koreaBook, chapter, verse, text));
            }

            br.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getBooks() {
        List<String> list = new ArrayList<>(data.keySet());
        Collections.sort(list);
        return list;
    }

    public List<Integer> getChapters(String book) {
        if (!data.containsKey(book)) return new ArrayList<>();
        List<Integer> list = new ArrayList<>(data.get(book).keySet());
        Collections.sort(list);
        return list;
    }

    public List<Verse> getVerses(String book, int chapter) {
        return data.get(book).get(chapter);
    }
}

