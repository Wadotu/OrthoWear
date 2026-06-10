package wadotu.orthodoxy.orthowear.presentation;

public class Verse {
    public String book;
    public int chapter;
    public int verse;
    public String text;

    public Verse(String book, int chapter, int verse, String text) {
        this.book = book;
        this.chapter = chapter;
        this.verse = verse;
        this.text = text;
    }

    @Override
    public String toString() {
        return verse + ". " + text;
    }
}
