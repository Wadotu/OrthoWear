package wadotu.orthodoxy.orthowear.presentation;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

public class LocaleHelper {

    private static final String SELECTED_LANGUAGE = "Locale.Helper.Selected.Language";

    public static void setLocale(Context context, String language) {
        persist(context, language);
        updateResources(language);
    }

    public static String getLanguage(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return preferences.getString(SELECTED_LANGUAGE, "en");
    }

    private static void persist(Context context, String language) {
        SharedPreferences preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        preferences.edit().putString(SELECTED_LANGUAGE, language).apply();
    }

    private static void updateResources(String language) {
        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(language);
        AppCompatDelegate.setApplicationLocales(appLocales);
    }
}
