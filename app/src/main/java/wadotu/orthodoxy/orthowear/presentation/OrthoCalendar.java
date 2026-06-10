package wadotu.orthodoxy.orthowear.presentation;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import wadotu.orthodoxy.orthowear.R;

public class OrthoCalendar {

    public interface CalendarCallback {
        void onDataLoaded(CalendarInfo info);
        void onError(String message);
    }

    public interface FullCalendarCallback {
        void onDataLoaded(OrthoDay day);
        void onError(String message);
    }

    public static class CalendarInfo {
        public String date;
        public String feast;
        public String fasting;
        public String dailyVerse;
        public String tone;
        public String readingBook;
        public int readingChapter;
    }

    private static Retrofit getRetrofit() {
        return new Retrofit.Builder()
                .baseUrl("https://orthocal.info/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static void getCalendarInfo(Context context, CalendarCallback callback) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        int mode = prefs.getInt("calendar_mode", 0); // 0: Revised Julian (gregorian), 1: Julian
        String apiMode = (mode == 0) ? "gregorian" : "julian";

        OrthoCalService service = getRetrofit().create(OrthoCalService.class);
        service.getToday(apiMode).enqueue(new Callback<OrthoDay>() {
            @Override
            public void onResponse(Call<OrthoDay> call, Response<OrthoDay> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Calendar cal = Calendar.getInstance();
                    if (mode == 1) cal.add(Calendar.DAY_OF_YEAR, -13);

                    CalendarInfo info = processDay(context, response.body(), mode, cal);
                    // Retrofit callback already runs on the main thread
                    callback.onDataLoaded(info);
                } else {
                    callback.onError("API Error");
                }
            }

            @Override
            public void onFailure(Call<OrthoDay> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public static void getFullCalendarDay(Context context, String apiMode, int year, int month, int day, FullCalendarCallback callback) {
        OrthoCalService service = getRetrofit().create(OrthoCalService.class);

        service.getDay(apiMode, year, month, day).enqueue(new Callback<OrthoDay>() {
            @Override
            public void onResponse(Call<OrthoDay> call, Response<OrthoDay> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onDataLoaded(response.body());
                } else {
                    callback.onError("API Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<OrthoDay> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public static CalendarInfo processDay(Context context, OrthoDay day, int mode, Calendar cal) {
        CalendarInfo info = new CalendarInfo();

        String currentLang = LocaleHelper.getLanguage(context);
        SimpleDateFormat sdf;
        if ("ko".equals(currentLang)) {
            sdf = new SimpleDateFormat("yyyy년 M월 d일 (EEEE)", Locale.KOREAN);
        } else {
            sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        }
        info.date = sdf.format(cal.getTime());

        // --- [Feast Selection Logic] ---
        StringBuilder feastBuilder = new StringBuilder();
        if (day.feasts != null && day.feasts.length > 0) {
            for (String f : day.feasts) {
                String translated = translateApiTitle(context, f);
                if (translated != null && !translated.isEmpty()) {
                    if (feastBuilder.length() > 0) feastBuilder.append("\n");
                    feastBuilder.append(translated);
                }
            }
        }

        if (feastBuilder.length() == 0 && day.titles != null) {
            for (String t : day.titles) {
                String translated = translateApiTitle(context, t);
                if (translated != null && !translated.isEmpty()) {
                    if (feastBuilder.length() > 0) feastBuilder.append("\n");
                    feastBuilder.append(translated);
                }
            }
        }

        if (feastBuilder.length() == 0) {
            info.feast = context.getString(R.string.no_feast);
        } else {
            String bestTitle = feastBuilder.toString();
            if (day.feast_level >= 7 && day.feast_level_description != null && !day.feast_level_description.isEmpty()) {
                String levelDesc = translateFeastLevel(context, day.feast_level_description);
                info.feast = bestTitle + " (" + levelDesc + ")";
            } else {
                info.feast = bestTitle;
            }
        }

        info.fasting = translateFasting(context, day);

        if (day.readings != null && !day.readings.isEmpty()) {

            // 복음경 → 사도경 → 사도행전 우선순위로 정렬
            List<String> priority = java.util.Arrays.asList("gospel", "epistle", "apostle", "acts");
            OrthoDay.Reading bestReading = day.readings.stream()
                    .min(java.util.Comparator.comparingInt(r -> {
                        String desc = r.description != null ? r.description.toLowerCase(Locale.ENGLISH) : "";
                        int idx = -1;
                        for (int i = 0; i < priority.size(); i++) {
                            if (desc.contains(priority.get(i))) { idx = i; break; }
                        }
                        return idx == -1 ? Integer.MAX_VALUE : idx;
                    }))
                    .orElse(day.readings.get(0));

            String desc = translateReadingDescription(context, bestReading.description);
            String rawDisplay = bestReading.display;
            String display = rawDisplay;

            if ("ko".equals(currentLang)) {
                if (bestReading.passage != null && !bestReading.passage.isEmpty()) {
                    String bookKey = bestReading.passage.get(0).book;
                    String koBook = BookNameMapper.get(bookKey);
                    if (!koBook.equals(bookKey)) {
                        display = rawDisplay.replaceFirst("^[A-Za-z0-9\\s\\.]+", koBook + " ");
                    }
                }
            }

            if (desc != null && !desc.isEmpty()) {
                info.dailyVerse = "[" + desc + "] " + display;
            } else {
                info.dailyVerse = display;
            }

            if (bestReading.passage != null && !bestReading.passage.isEmpty()) {
                info.readingBook = BookNameMapper.get(bestReading.passage.get(0).book);
                info.readingChapter = bestReading.passage.get(0).chapter;
            }

        } else {
            info.dailyVerse = "";
        }

        if (day.tone > 0) {
            info.tone = context.getString(R.string.tone, day.tone);
        } else {
            info.tone = "";
        }

        return info;
    }

    public static String translateFasting(Context context, OrthoDay day) {
        if (day == null) return "";

        String desc = day.fast_level_desc != null ? day.fast_level_desc.toLowerCase(Locale.ENGLISH) : "";
        String exc = day.fast_exception_desc != null ? day.fast_exception_desc.toLowerCase(Locale.ENGLISH) : "";

        if (desc.contains("no fast") || desc.contains("fast free") || day.fast_level == 0) {
            return context.getString(R.string.fast_free);
        }

        if (exc.contains("fish")) return context.getString(R.string.fish_allowed);
        if (exc.contains("wine") || exc.contains("oil")) return context.getString(R.string.wine_oil_allowed);
        if (desc.contains("fast")) return context.getString(R.string.fast_day);

        return day.fast_level_desc;
    }

    public static String translateReadingDescription(Context context, String desc) {
        if (desc == null) return "";
        String lower = desc.toLowerCase(Locale.ENGLISH);
        if (lower.contains("gospel")) return context.getString(R.string.gospel);
        if (lower.contains("epistle")) return context.getString(R.string.epistle);
        if (lower.contains("apostle")) return context.getString(R.string.apostles);
        if (lower.contains("acts")) return context.getString(R.string.acts);
        return desc;
    }

    public static String translateFeastLevel(Context context, String desc) {
        if (desc == null) return "";
        if (!"ko".equals(LocaleHelper.getLanguage(context))) return desc;

        String lower = desc.toLowerCase(Locale.ENGLISH);
        if (lower.contains("major feast")) return "대축일";
        if (lower.contains("great feast")) return "대축일";
        if (lower.contains("saint of the day")) return "오늘의 성인";
        return desc;
    }

    public static String translateApiTitle(Context context, String apiTitle) {
        if (apiTitle == null) return "";
        // 명시적으로 영문 로케일 적용하여 다국어 환경에서의 소문자 변환 오류 방지
        String lower = apiTitle.toLowerCase(Locale.ENGLISH);

        // --- [스마트 필터링] ---
        if (lower.contains("sunday after") || lower.contains("monday of") ||
                lower.contains("tuesday of") || lower.contains("wednesday of") ||
                lower.contains("thursday of") || lower.contains("friday of") ||
                lower.contains("saturday of") || lower.contains("week of")) {

            if (!lower.contains("all saints") && !lower.contains("pascha") &&
                    !lower.contains("pentecost") && !lower.contains("spirit")) {
                return "";
            }
            // 여기서 return apiTitle; 하던 부분을 제거하여
            // 주요 축일 문구가 포함되어 있을 시 아래쪽의 한국어 번역 분기를 탈 수 있도록 허용 (Fall-through)
        }

        String currentLang = LocaleHelper.getLanguage(context);

        if ("ko".equals(currentLang)) {
            // [A 구역] 대축일 매핑
            if (apiTitle.equals("Holy Pascha")) return context.getString(R.string.pascha_feast);
            if (lower.contains("nativity") && lower.contains("lord")) return context.getString(R.string.nativity_feast);
            if (lower.contains("theophany") || lower.contains("baptism")) return context.getString(R.string.theophany_feast);
            if (apiTitle.contains("Feast of the Holy Trinity")) return context.getString(R.string.pentecost_feast);
            if (lower.contains("ascension")) return context.getString(R.string.ascension_feast);
            if (lower.contains("annunciation")) return context.getString(R.string.annunciation);
            if (lower.contains("transfiguration")) return context.getString(R.string.transfiguration);
            if (lower.contains("dormition") && lower.contains("theotokos")) return context.getString(R.string.dormition);
            if (lower.contains("palm sunday") || lower.contains("entry into jerusalem")) return context.getString(R.string.palm_sunday_feast);
            if (lower.contains("elevation") && lower.contains("cross")) return context.getString(R.string.elevation_cross);
            if (lower.contains("presentation") && lower.contains("lord")) return context.getString(R.string.presentation_christ);
            if (lower.contains("presentation") && lower.contains("theotokos")) return context.getString(R.string.presentation_theotokos);
            if (lower.contains("nativity") && lower.contains("theotokos")) return context.getString(R.string.nativity_theotokos);

            // [주일 및 이동 축일]
            if (lower.contains("all saints")) return "모든 성인들의 주일";
            if (apiTitle.contains("Day of the Holy Spirit")) return "성령 축일";
            if (lower.contains("publican") && lower.contains("pharisee")) return "세리와 바리사이파 사람 주일";
            if (lower.contains("prodigal son")) return "탕자 주일";
            if (lower.contains("last judgment")) return "심판 주일 (금육 주일)";
            if (lower.contains("forgiveness")) return "용서 주일 (유식 주일)";
            if (lower.contains("orthodoxy")) return "정교 주일";
            if (lower.contains("gregory palamas")) return "성 그레고리오스 팔라마스 주일";
            if (lower.contains("veneration of the cross")) return "십자가 경배 주일";
            if (lower.contains("john of the ladder")) return "성 요한 클리마쿠스 주일";
            if (lower.contains("mary of egypt")) return "이집트의 성녀 마리아 주일";
            if (lower.contains("myrrhbearers")) return "향유가진 여인 주일";
            if (lower.contains("paralytic")) return "중풍 병자 주일";
            if (lower.contains("samaritan woman")) return "사마리아 여인 주일";
            if (lower.contains("blind man")) return "소경 주일";
            if (lower.contains("fathers of the first ecumenical council")) return "제1차 세계 공의회 참석 교부들 주일";

            // [1월]
            if (lower.contains("basil") && lower.contains("great")) return "성 대 바실리오스 게사리아의 대주교";
            if (lower.contains("seraphim") && lower.contains("sarov")) return "성 세라핌 사로프의 수도자";
            if (lower.contains("sylvester")) return "성 실베스트로스 로마의 대주교";
            if (lower.contains("apollinaria")) return "성 아폴리나리아 수녀";
            if (lower.contains("synclitica")) return "성 싱글리띠끼 수녀";
            if (lower.contains("domnica")) return "성 돔니끼 수녀";
            if (lower.contains("gregory") && lower.contains("nyssa")) return "성 그레고리 니사의 주교";
            if (lower.contains("theodosius") && lower.contains("cenobiarch")) return "성 테오도시오스 수도원장";
            if (lower.contains("sabbas") && lower.contains("serbia")) return "성 사바스 세르비아의 대주교";
            if (lower.contains("tatiana")) return "성 따띠안나 순교자";
            if (lower.contains("hermylus") && lower.contains("stratonicus")) return "성 에르밀로스와 성 스트라토니꼬스 순교자";
            if (lower.contains("nina")) return "성 니나 순교자";
            if (lower.contains("paul") && lower.contains("thebes")) return "성 바울로 티바의 수도자";
            if (lower.contains("anthony") && lower.contains("great")) return "성 대 안토니오스 수도자";
            if (lower.contains("athanasius") && lower.contains("cyril")) return "성 대 아타나시오스와 성 끼릴로스";
            if (lower.contains("makarios") && lower.contains("egypt")) return "성 마카리오스 수도자";
            if (lower.contains("mark") && lower.contains("ephesus")) return "성 마르코 에페소의 대주교";
            if (lower.contains("euthymius") && lower.contains("great")) return "성 에프티미오스 대수도자";
            if (lower.contains("maximus") && lower.contains("confessor")) return "성 막시모스 고백자";
            if (lower.contains("maxim") && lower.contains("greek")) return "성 막심 그리스인 수도자";
            if (lower.contains("timothy") && lower.contains("apostle")) return "성 디모테오스 사도";
            if (lower.contains("xenia")) return "성 크세니 수녀";
            if (lower.contains("gregory") && lower.contains("theologian")) return "성 그레고리 신학자";
            if (lower.contains("nicholas") && lower.contains("royal family")) return "짜르 니꼴라스 II세와 황실 가족";
            if (lower.contains("elizabeth") && lower.contains("duchess")) return "성 엘리자베스 공작 부인 순교자";
            if (lower.contains("xenophon")) return "성 크세노폰 수도자";
            if (lower.contains("relop") && lower.contains("chrysostom")) return "성 요한 크리소스톰 이장기념";
            if (lower.contains("ephrem") && lower.contains("syrian")) return "성 에프렘 시리아인 수도자";
            if (lower.contains("charis")) return "성 하리스 순교자";
            if (lower.contains("cyrus") && lower.contains("john")) return "성 끼로스와 요한 자선치료자";

            // [2월]
            if (lower.contains("tryphon")) return "성 트리폰 순교자";
            if (lower.contains("meeting") && lower.contains("lord")) return "주님의 입당 축일";
            if (lower.contains("nicholas") && lower.contains("japan")) return "성 니꼴라스 일본의 대주교";
            if (lower.contains("isidore") && lower.contains("pelusium")) return "성 이시도로스 수도자";
            if (lower.contains("agatha")) return "성 아가티 순교자";
            if (lower.contains("photius") && lower.contains("constantinople")) return "성 포티오스 콘스탄티노플의 총대주교";
            if (lower.contains("theodore") && lower.contains("stratilates")) return "성 테오도로스 대순교자";
            if (lower.contains("nikephoros")) return "성 니키포로스 순교자";
            if (lower.contains("charalambos")) return "성 하랄람보스 사제순교자";
            if (lower.contains("blaise")) return "성 블라시오스 사제순교자";
            if (lower.contains("theodora") && lower.contains("empress")) return "성 떼오도라 왕비수녀";
            if (lower.contains("auxentios")) return "성 아브크센티오스 사제수도자";
            if (lower.contains("anthimus") && lower.contains("chios")) return "성 안티무스 히오스의 주교";
            if (lower.contains("onesimus")) return "성 오네시모스 사도";
            if (lower.contains("pamphilus")) return "성 빰필로스 순교자";
            if (lower.contains("theodore") && lower.contains("tyro")) return "성 떼오도로스 대순교자";
            if (lower.contains("leo") && lower.contains("rome")) return "성 레온 로마의 주교";
            if (lower.contains("philothei")) return "성 필로테이 수녀순교자";
            if (lower.contains("polycarp")) return "성 폴리카르포스 주교순교자";
            if (lower.contains("finding") && lower.contains("baptist")) return "세례요한의 참수당한 머리 발견 기념일";
            if (lower.contains("tarasius")) return "성 타라시오스 콘스탄티노플의 총대주교";
            if (lower.contains("photini")) return "성 포티니 대순교자";
            if (lower.contains("prokopios") && lower.contains("confessor")) return "성 쁘로코피오스 증거자";
            if (lower.contains("raphael") && lower.contains("brooklyn")) return "성 라파엘 브루클린의 주교";
            if (lower.contains("kyranna")) return "성 끼란나 근대순교자";
            if (lower.contains("cassian")) return "성 까시아노스 수사증거자";

            // [3월]
            if (lower.contains("evdokia")) return "성 에브도끼아 수녀순교자";
            if (lower.contains("nicholas") && lower.contains("planas")) return "성 니꼴라스 플라나스 사제";
            if (lower.contains("gerasimos") && lower.contains("jordan")) return "성 예라시모스 요르단의 수도자";
            if (lower.contains("konon")) return "성 코논 수사순교자";
            if (lower.contains("42 martyrs") && lower.contains("amorion")) return "아모리오의 42인 순교자들";
            if (lower.contains("40 martyrs") && lower.contains("sebaste")) return "세바스티아의 40인 순교자들";
            if (lower.contains("anastasia") && lower.contains("patrician")) return "성 아나스타시아 수녀";
            if (lower.contains("theodora") && lower.contains("arta")) return "성 테오도라 아르타의 왕비수녀";
            if (lower.contains("sophronios") && lower.contains("jerusalem")) return "성 소프로니오스 예루살렘의 총대주교";
            if (lower.contains("benedict") && lower.contains("nursia")) return "성 베네딕도스 수도자";
            if (lower.contains("sabinos")) return "성 사비노스 순교자";
            if (lower.contains("alexios") && lower.contains("man of god")) return "성 알렉시오스 수도자";
            if (lower.contains("chrysanthos") && lower.contains("daria")) return "성 흐리산토스와 성 다리아 순교자";
            if (lower.contains("nikon") && lower.contains("199")) return "성 니콘 사제순교자와 그의 199인 제자순교자들";
            if (lower.contains("artemon")) return "성 아르테몬 사제순교자";
            if (lower.contains("lydia")) return "성 리디아 순교자";
            if (lower.contains("john") && lower.contains("climax")) return "성 요한 끌리막스 시나이의 수도자";
            if (lower.contains("innocent") && lower.contains("alaska")) return "성 이노켄티오스 모스크바의 대주교";

            // [4월]
            if (lower.contains("mary") && lower.contains("egypt")) return "성 마리아 에집트의 수녀";
            if (lower.contains("amphianos") && lower.contains("edesios")) return "성 암피아누스와 성 에데시우스 순교자";
            if (lower.contains("joseph") && lower.contains("hymnographer")) return "성 요셉 사제 성가작가";
            if (lower.contains("nikitas") && lower.contains("medikion")) return "성 니끼따스 수사증거자";
            if (lower.contains("platon") && lower.contains("sakkoudion")) return "성 플라톤 수도자";
            if (lower.contains("theodora") && lower.contains("thessalonica")) return "성 떼오도라 데살로니끼의 수녀";
            if (lower.contains("gregory") && lower.contains("sinaite")) return "성 그레고리 시나이인";
            if (lower.contains("eutychios") && lower.contains("constantinople")) return "성 에프티히오스 콘스탄티노플의 총대주교";
            if (lower.contains("sabbas") && lower.contains("kalymnos")) return "성 사바스 깔림노스의 주교";
            if (lower.contains("raphael") && lower.contains("nicholas") && lower.contains("irene")) return "성 라파엘, 성 니꼴라스, 성 이리니 미틸리니의 순교자들";
            if (lower.contains("acacius") && lower.contains("kapsokalyvia")) return "성 아까끼오스 수사";
            if (lower.contains("martin") && lower.contains("rome")) return "성 마르티노 로마의 주교";
            if (lower.contains("crescens")) return "성 크레스켄스 미라의 순교자";
            if (lower.contains("agape") && lower.contains("irene") && lower.contains("chionia")) return "성 아가삐, 이리니, 효니아 세 자매 순교자들";
            if (lower.contains("sabbas") && lower.contains("goth")) return "성 사바스 고트인 순교자";
            if (lower.contains("athanasios") && lower.contains("meteoron")) return "성 아타나시오스 메떼오라의 구세주 변모 수도원 설립자";
            if (lower.contains("theodore") && lower.contains("sykeon")) return "성 떼오도로스 시케온";
            if (lower.contains("george") && lower.contains("trophy")) return "성 게오르기오스 대순교자";
            if (lower.contains("mark") && lower.contains("evangelist")) return "성 마르코스 사도";
            if (lower.contains("basil") && lower.contains("amasia")) return "성 바실리오스 아마시아의 주교순교자";
            if (lower.contains("john") && lower.contains("kathara")) return "성 요한 수사증거자";
            if (lower.contains("jason") && lower.contains("sosipater")) return "성 이아손 사도";
            if (lower.contains("ignatios") && lower.contains("stavropol")) return "성 이그나티오스 스타브로플의 주교";

            // [5월]
            if (lower.contains("jeremiah") && lower.contains("prophet")) return "성 예레미야 예언자";
            if (lower.contains("timothy") && lower.contains("maura")) return "성 디모테오와 마브라 순교자";
            if (lower.contains("pelagia") && lower.contains("tarsus")) return "성 벨라기아 순교자";
            if (lower.contains("irene") && lower.contains("martyr")) return "성 이리니 대순교자";
            if (lower.contains("job") && lower.contains("righteous")) return "욥 의인";
            if (lower.contains("john") && lower.contains("theologian")) return "성 요한 사도 복음자";
            if (lower.contains("isaiah") && lower.contains("prophet")) return "성 이사야 예언자";
            if (lower.contains("simon") && lower.contains("zealot")) return "성 시몬 사도";
            if (lower.contains("cyril") && lower.contains("methodios")) return "성 끼릴로스와 성 메토디오스 선교사";
            if (lower.contains("epiphanios") && lower.contains("cyprus")) return "성 에피파니오스 키프로스의 주교";
            if (lower.contains("germanos") && lower.contains("constantinople")) return "성 예르마노스 콘스탄티노플의 총대주교";
            if (lower.contains("glykeria")) return "성 글리게리아 순교자";
            if (lower.contains("isidore") && lower.contains("chios")) return "성 이시도로스 순교자";
            if (lower.contains("pachomios") && lower.contains("great")) return "성 파코미오스 대수도자";
            if (lower.contains("peter") && lower.contains("lampsakos")) return "성 베드로외 4인 순교자들";
            if (lower.contains("patrick") && lower.contains("prusa")) return "성 빠드리기오스 순교자";
            if (lower.contains("constantine") && lower.contains("helen")) return "성 콘스탄티노스와 성 엘레니 준사도";
            if (lower.contains("symeon") && lower.contains("wonderful")) return "성 시메온 수사";
            if (lower.contains("john") && lower.contains("russian")) return "성 요한 러시아인 수사 증거자";
            if (lower.contains("theodosia") && lower.contains("constantinople")) return "성 떼오도시아 수녀순교자";
            if (lower.contains("isaac") && lower.contains("dalmatian")) return "성 이사아기오스 수사";

            // [6월]
            if (lower.contains("justin") && lower.contains("philosopher")) return "성 유스티노스 순교철학자";
            if (lower.contains("nikephoros") && lower.contains("constantinople")) return "성 니키포로스 주교증거자";
            if (lower.contains("fathers") && lower.contains("1st council")) return "제 1차 세계공의회 참석교부들";
            if (lower.contains("panagis") && lower.contains("kephalonia")) return "성 빠나기스 사제";
            if (lower.contains("theophanes") && lower.contains("constantinople")) return "성 테오판 근대순교자";
            if (lower.contains("cyril") && lower.contains("alexandria")) return "성 끼릴로스 알렉산드리아의 총대주교";
            if (lower.contains("bartholomew") && lower.contains("apostle")) return "성 바르톨로메오스 사도";
            if (lower.contains("peter") && lower.contains("athos")) return "아토스 성산의 성 베드로";
            if (lower.contains("holy spirit")) return "성령축일";
            if (lower.contains("elisha") && lower.contains("prophet")) return "성 엘리사 예언자";
            if (lower.contains("jerome")) return "성 제롬";
            if (lower.contains("tychon") && lower.contains("amathus")) return "성 티혼 주교";
            if (lower.contains("manuel") && lower.contains("sabel")) return "성 마누엘 순교자";
            if (lower.contains("kallistos") && lower.contains("constantinople")) return "성 칼리스토스 콘스탄티노플의 총대주교";
            if (lower.contains("nicholas") && lower.contains("kabasylas")) return "성 니콜라스 까바실라스";
            if (lower.contains("alban") && lower.contains("britain")) return "성 알반";
            if (lower.contains("eusebios") && lower.contains("samosata")) return "성 에브세비오스 사제순교자";
            if (lower.contains("nativity") && lower.contains("baptist")) return "성 세례자 요한의 탄생";
            if (lower.contains("peter") && lower.contains("febronia")) return "성 베드로 무롬의 수도자";
            if (lower.contains("samson") && lower.contains("hospitable")) return "성 삼손 수도자";
            if (lower.contains("peter") && lower.contains("paul")) return "성 베드로와 성 바울로 사도";

            // [7월]
            if (lower.contains("juvenal") && lower.contains("jerusalem")) return "성 유베날리오 예루살렘의 총대주교";
            if (lower.contains("anatolius") && lower.contains("constantinople")) return "성 아나톨리오스 콘스탄티노플의 총대주교";
            if (lower.contains("andrew") && lower.contains("crete")) return "성 안드레아 크레테의 대주교";
            if (lower.contains("athanasios") && lower.contains("athos")) return "성 아타나시오스 아토스산의 수도자";
            if (lower.contains("sisoe") && lower.contains("great")) return "성 시소이스 대수도자";
            if (lower.contains("kyriaki") && lower.contains("martyr")) return "성 끼리아끼 대순교자";
            if (lower.contains("prokopios") && lower.contains("great")) return "성 쁘로코피오스 대순교자";
            if (lower.contains("fathers") && lower.contains("4th council")) return "제 4차 세계공의회에 참석한 교부들";
            if (lower.contains("nikodemos") && lower.contains("hagiorite")) return "성 니꼬디모스 아토스산의 수도자";
            if (lower.contains("vladimir") && lower.contains("equal")) return "성 블라디미르 준사도";
            if (lower.contains("athinogenis")) return "성 아티노게니스 주교순교자";
            if (lower.contains("marina") && lower.contains("great")) return "성 마리나 대순교자";
            if (lower.contains("elizabeth") && lower.contains("princess")) return "성 엘리사벹 뻬쩨르부르크의 공주";
            if (lower.contains("elias") && lower.contains("prophet")) return "엘리야 예언자";
            if (lower.contains("john") && lower.contains("symeon") && lower.contains("fools")) return "성 요한과 성 시메온 수도자";
            if (lower.contains("mary magdalene")) return "막달라 마리아";
            if (lower.contains("boris") && lower.contains("gleb")) return "성 보리스 순교자";
            if (lower.contains("christina") && lower.contains("tyre")) return "성 크리스티나 순교자";
            if (lower.contains("olivia")) return "올리비아 봉사자";
            if (lower.contains("paraskevi") && lower.contains("rome")) return "성 빠라스께비 순교자";
            if (lower.contains("panteleimon")) return "성 빤델레이몬 대순교자";
            if (lower.contains("kallinikos") && lower.contains("gangra")) return "성 갈리니꼬스 순교자";
            if (lower.contains("theodoti")) return "성 떼오도띠 순교자";
            if (lower.contains("joseph") && lower.contains("arimathea")) return "성 요셉 아리마태아인";

            // [8월]
            if (lower.contains("maccabees")) return "마카베오의 7인 순교자들과 그 어머니 솔로모니";
            if (lower.contains("seven sleepers") && lower.contains("ephesus")) return "에페소의 7인 순교자들";
            if (lower.contains("nonna")) return "성 논나";
            if (lower.contains("laurence") && lower.contains("rome")) return "성 라브렌티오스 대보제 순교자";
            if (lower.contains("niphon") && lower.contains("constantinople")) return "성 니폰 콘스탄티노플의 총대주교";
            if (lower.contains("susanna") && lower.contains("martyr")) return "성 수산나 순교자";
            if (lower.contains("photius") && lower.contains("anikitos")) return "성 포티오스와 아니끼도스 순교자";
            if (lower.contains("tikhon") && lower.contains("zadonsk")) return "짜돈스크의 성 티콘";
            if (lower.contains("maximus") && lower.contains("confessor")) return "모스크바의 막시모스 증거자";
            if (lower.contains("myron")) return "성 미론 순교자";
            if (lower.contains("phloros") && lower.contains("lauros")) return "성 플로로스와 성 라브로스 순교자";
            if (lower.contains("andrew") && lower.contains("commander")) return "성 안드레아스 군인순교자";
            if (lower.contains("samuel") && lower.contains("prophet")) return "성 사무엘 예언자";
            if (lower.contains("anthusa")) return "성 안투사 순교자";
            if (lower.contains("kosmas") && lower.contains("aitolian")) return "성 꼬즈마 에똘리아인 사제순교자";
            if (lower.contains("titus") && lower.contains("apostle")) return "성 디도스 70인 사도";
            if (lower.contains("adrian") && lower.contains("natalia")) return "성 아드리아노스와 성 나탈리아 순교자";
            if (lower.contains("pimen") && lower.contains("great")) return "성 대 삐민";
            if (lower.contains("moses") && lower.contains("black")) return "성 모이시 에티오피아인 수도자";
            if (lower.contains("beheading") && lower.contains("baptist")) return "성 요한 선구자 참수 기념일";
            if (lower.contains("alexander") && lower.contains("constantinople")) return "성 알렉산드로스 콘스탄티노플의 총대주교";

            // [9월]
            if (lower.contains("symeon") && lower.contains("stylite")) return "성 시메온 기둥위의 수도자";
            if (lower.contains("40 virgin martyrs")) return "40인 여자 순교자들과 성 암몬 보제 순교자";
            if (lower.contains("mamas")) return "성 마마스 순교자";
            if (lower.contains("anthimos") && lower.contains("nicomedia")) return "성 안티모스 주교순교자";
            if (lower.contains("gorazd")) return "성 고라즈드 주교순교자";
            if (lower.contains("miracle") && lower.contains("michael") && lower.contains("chonae")) return "미하일 대천사 기적기념일";
            if (lower.contains("sozon")) return "성 소존 순교자";
            if (lower.contains("menodora") && lower.contains("metrodora")) return "성 미노도라, 미트로도라, 님포도라 세 자매 순교자들";
            if (lower.contains("euphrosynos") && lower.contains("cook")) return "성 에프로시노스 수도자";
            if (lower.contains("autonomos")) return "성 아프토노모스 주교순교자";
            if (lower.contains("cornelius") && lower.contains("centurion")) return "고르넬리오스 백인대장";
            if (lower.contains("fathers") && lower.contains("6th council")) return "제 6차 세계공의회 참석 교부들";
            if (lower.contains("euphemia") && lower.contains("great")) return "성 에피미아 대순교자";
            if (lower.contains("sophia") && lower.contains("faith") && lower.contains("hope") && lower.contains("love")) return "성 소피아 순교자와 그녀의 세 딸 삐스띠스, 엘삐다, 아가삐 순교자";
            if (lower.contains("eustathios") && lower.contains("placidas")) return "성 에브스타티오스 대순교자";
            if (lower.contains("jonah") && lower.contains("prophet")) return "요나 예언자";
            if (lower.contains("kosmas") && lower.contains("zographou")) return "성 꼬즈마 수도자";
            if (lower.contains("26 martyrs") && lower.contains("zographou")) return "26명의 수도자들과 순교자들";
            if (lower.contains("silouan") && lower.contains("athos")) return "성 실루아노스 아토스산의 수도자";
            if (lower.contains("thekla") && lower.contains("equal")) return "성 테클라 대순교자";
            if (lower.contains("sergius") && lower.contains("radonezh")) return "성 세르기우스 수도사제";
            if (lower.contains("kallistratos")) return "성 칼리스트라토스 순교자";
            if (lower.contains("gregory") && lower.contains("enlightener")) return "성 그레고리오스 아르메니아의 주교순교자";

            // [10월]
            if (lower.contains("protection") && lower.contains("theotokos")) return "성모님 보호축일";
            if (lower.contains("john") && lower.contains("koukouzelis")) return "성 요한 꾸꾸젤리스 수도자";
            if (lower.contains("romanus") && lower.contains("melodist")) return "성 로마누스 성가작가";
            if (lower.contains("cyprian") && lower.contains("justina")) return "성 기쁘리아노스 주교순교자, 유스티나 순교자";
            if (lower.contains("dionysios") && lower.contains("areopagite")) return "성 디오니시오스 아레오파고의 주교순교자";
            if (lower.contains("ammon") && lower.contains("egypt")) return "성 암몬 이집트인 수도자";
            if (lower.contains("charitina")) return "성 하리티니 순교자";
            if (lower.contains("thomas") && lower.contains("apostle")) return "성 토마 사도";
            if (lower.contains("sergius") && lower.contains("bacchus")) return "성 세르기오스와 박호스 대순교자";
            if (lower.contains("pelagia") && lower.contains("antioch")) return "성 벨라기아 수녀";
            if (lower.contains("abraham") && lower.contains("lot")) return "아브라함과 롯 의인";
            if (lower.contains("eulampios") && lower.contains("eulampia")) return "성 에블람비오스와 에블람비아 순교자";
            if (lower.contains("theophanes") && lower.contains("graptus")) return "성 테오판 성가작가";
            if (lower.contains("andronicus") && lower.contains("tarachos")) return "성 안드로니꼬스와 타라호스 순교자";
            if (lower.contains("chryse")) return "성 흐리시 순교자";
            if (lower.contains("kosmas") && lower.contains("maiuma")) return "성 꼬즈마 성가작가";
            if (lower.contains("ignatios") && lower.contains("methymna")) return "성 이그나티오스 메팀나의 주교";
            if (lower.contains("lucian") && lower.contains("antioch")) return "성 루끼아노스 사제순교자";
            if (lower.contains("hosea") && lower.contains("prophet")) return "호세아 예언자";
            if (lower.contains("luke") && lower.contains("evangelist")) return "성 루가 복음사도";
            if (lower.contains("john") && lower.contains("rila")) return "성 요한 불가리아의 릴라 수도원 설립자";
            if (lower.contains("matrona") && lower.contains("chios")) return "성 마트로나 수녀";
            if (lower.contains("gerasimos") && lower.contains("kephalonia")) return "성 예라시모스 수도자";
            if (lower.contains("hilarion") && lower.contains("great")) return "성 일라리온 대수도자";
            if (lower.contains("abercius") && lower.contains("equal")) return "성 아베르기오스 준사도";
            if (lower.contains("james") && lower.contains("brother") && lower.contains("lord")) return "주님의 형제 성 야고보 사도";
            if (lower.contains("marcianos") && lower.contains("martyrios")) return "성 마르기아노스와 마르티리오스 순교자";
            if (lower.contains("demetrios") && lower.contains("myrrh")) return "성 디미뜨리오스 대순교자";
            if (lower.contains("nestor")) return "성 네스토르 순교자";
            if (lower.contains("anastasia") && lower.contains("roman")) return "성 아나스타시아 로마인 수녀순교자";
            if (lower.contains("helen") && lower.contains("serbia")) return "성 헬렌 세르비아의 왕비";

            // [11월]
            if (lower.contains("cosmas") && lower.contains("damian") && lower.contains("asia")) return "성 꼬즈마와 성 다미아노스 자선치료자";
            if (lower.contains("akindynos") && lower.contains("pegasios")) return "성 아킨디노스, 비가시오, 엘피도포로스, 아프토니오스, 아넴포디스토스 순교자들";
            if (lower.contains("akepsimas") && lower.contains("joseph") && lower.contains("aithalas")) return "성 아켑시마, 요세프, 아이탈라 순교자";
            if (lower.contains("ioannikios") && lower.contains("great")) return "성 요아니끼오스 수도자";
            if (lower.contains("galaktion") && lower.contains("episteme")) return "성 갈락티온과 에피스티미 순교자";
            if (lower.contains("synaxis") && lower.contains("archangels")) return "미하엘, 가브리엘 대천사들과 천상의 모든 천사들";
            if (lower.contains("nectarios") && lower.contains("aegina")) return "성 넥따리오스 대주교";
            if (lower.contains("theoktisti") && lower.contains("lesbos")) return "성 테옥티스티 메팀나의 수녀";
            if (lower.contains("arsenios") && lower.contains("cappadocia")) return "성 아르세니오스 수도자";
            if (lower.contains("menas") && lower.contains("egypt")) return "성 미나스 대수도자";
            if (lower.contains("john") && lower.contains("merciful")) return "성 요한 알렉산드리아의 총대주교";
            if (lower.contains("philip") && lower.contains("apostle")) return "성 필립보 사도";
            if (lower.contains("gurias") && lower.contains("samonas")) return "성 구리아, 사모나, 아비보 순교자들";
            if (lower.contains("matthew") && lower.contains("evangelist")) return "성 마태오 복음사도";
            if (lower.contains("gregory") && lower.contains("neocaesarea")) return "성 그레고리오스 네오케사리아의 주교";
            if (lower.contains("proclus") && lower.contains("constantinople")) return "성 브로클로 콘스탄티노플의 총대주교";
            if (lower.contains("philemon") && lower.contains("apostle")) return "성 필레몬 사도";
            if (lower.contains("cecilia") && lower.contains("valerian")) return "성 기킬리아, 발레리안, 티부르티오스, 막시무스 순교자";
            if (lower.contains("alexander") && lower.contains("nevsky")) return "성 알렉산더 네프스키 러시아의 대공";
            if (lower.contains("clement") && lower.contains("rome")) return "성 클리멘트 로마의 주교순교자";
            if (lower.contains("stylianos") && lower.contains("paphlagonia")) return "성 스틸리아노스 수도자";
            if (lower.contains("stephen") && lower.contains("new")) return "성 스테파노 증거자(순교자)";
            if (lower.contains("paramonos") && lower.contains("370")) return "성 빠라모노스와 370인 순교자들";
            if (lower.contains("andrew") && lower.contains("first")) return "성 안드레아 첫 사도";

            // [12월]
            if (lower.contains("philaret") && lower.contains("merciful")) return "성 필라레토스 수사";
            if (lower.contains("habakkuk") && lower.contains("prophet")) return "성 하바꾹 예언자";
            if (lower.contains("john") && lower.contains("damascus")) return "성 요한 다마스커스인";
            if (lower.contains("barbara") && lower.contains("great")) return "성 바르바라 대순교자";
            if (lower.contains("sabbas") && lower.contains("sanctified")) return "성 사바스 수도자";
            if (lower.contains("ambrose") && lower.contains("milan")) return "성 암브로시오스 주교";
            if (lower.contains("patapius") && lower.contains("thebes")) return "성 빠따삐오스 수도자";
            if (lower.contains("conception") && lower.contains("anna")) return "성모님의 모친 안나 잉태일";
            if (lower.contains("herman") && lower.contains("alaska")) return "성 헤르만 알라스카의 선교사";
            if (lower.contains("auxentios") && lower.contains("eugenios") && lower.contains("mardarios")) return "성 아프크센티오스 외 4인 순교자";
            if (lower.contains("eleutherios") && lower.contains("illyricum")) return "성 엘레프테리오스 주교순교자";
            if (lower.contains("haggai") && lower.contains("prophet")) return "하깨 예언자";
            if (lower.contains("dionysios") && lower.contains("zakynthos")) return "성 디오니시오스 대주교";
            if (lower.contains("daniel") && lower.contains("prophet")) return "다니엘 예언자";
            if (lower.contains("ignatios") && lower.contains("god-bearer")) return "성 이그나티오스 안티오키아의 대주교";
            if (lower.contains("john") && lower.contains("kronstadt")) return "성 요한 크론스타트 사제";
            if (lower.contains("anastasia") && lower.contains("deliverer")) return "성 아나스타시아 대순교자";
            if (lower.contains("eugenia")) return "성 에브게니아 수녀순교자";
            if (lower.contains("stephen") && lower.contains("archdeacon")) return "스테파노스 대보제 첫 순교자";
            if (lower.contains("simon") && lower.contains("myrrh")) return "성 시몬 아토스산의 수도자";
            if (lower.contains("anysia")) return "성 아니시아 수녀순교자";
            if (lower.contains("melania") && lower.contains("roman")) return "성 멜라니 로마인 수녀";

            // [기존 주요 성인 및 명칭 유지]
            if (lower.contains("nicholas") && lower.contains("myra")) return "미라의 대주교 성 니콜라스";
            if (lower.contains("païsios") && lower.contains("holy mountain")) return "성산의 성 파이시오스 수사";
            if (lower.contains("dionysios") && lower.contains("aegina")) return "성 디오니시오스 애기나의 대주교";
            if (lower.contains("nektarios")) return "애기나의 성 넥타리오스 주교";
            if (lower.contains("george") && lower.contains("great")) return "성 게오르기오스 대순교자";
            if (lower.contains("panteleimon")) return "성 빤델레이몬 대순교자";
            if (lower.contains("catherine")) return "성 카테리나 대순교자";
            if (lower.contains("spyridon")) return "트리미투스의 성 스피리돈 주교";
            if (lower.contains("chrysostom")) return "성 요한 크리소스토모스 콘스탄티노플 총대주교";
            if (lower.contains("basil") && lower.contains("great")) return "성 대 바실리오스 대주교";
            if (lower.contains("constantine") && lower.contains("helen")) return "성 콘스탄티노스 대제와 성 엘레니 사도대등자";
            if (lower.contains("silouan")) return "성산의 성 실루아노스 수사";
            if (lower.contains("seraphim") && lower.contains("sarov")) return "사로브의 성 세라핌 수사";
            if (lower.contains("mary magdalene")) return "성 마리아 막달레나 사도대등자";
            if (lower.contains("peter") && lower.contains("paul")) return "으뜸 사도 베드로와 바울로";
            if (lower.contains("andrew") && lower.contains("called")) return "사도 안드레아";

        } else if ("el".equals(currentLang)) {
            if (lower.contains("basil") && lower.contains("great")) return "Μέγας Βασίλειος";
            if (lower.contains("seraphim") && lower.contains("sarov")) return "Όσιος Σεραφείμ του Σάρωφ";
            if (lower.contains("nicholas") && lower.contains("myra")) return "Άγιος Νικόλαος ο Θαυματουργός";
            if (lower.contains("george") && lower.contains("trophy")) return "Άγιος Γεώργιος ο Τροπαιοφόρος";
            if (lower.contains("panteleimon")) return "Άγιος Παντελεήμων ο Ιαματικός";
            if (lower.contains("catherine")) return "Αγία Αικατερίνη η Μεγαλομάρτυς";
            if (lower.contains("theophany") || lower.contains("baptism")) return "Τα Άγια Θεοφάνεια";
            if (lower.contains("pascha")) return "Το Άγιο Πάσχα";
            if (lower.contains("dormition")) return "Η Κοίμησις της Θεοτόκου";
            if (lower.contains("nativity") && lower.contains("theotokos")) return "Το Γενέσιον της Θεοτόκου";
        } else if ("cu".equals(currentLang)) {
            if (lower.contains("basil") && lower.contains("great")) return "Василий Великий";
            if (lower.contains("seraphim") && lower.contains("sarov")) return "Преп. Серафим Саровский";
            if (lower.contains("nicholas") && lower.contains("myra")) return "Св. Николай Чудотворец";
            if (lower.contains("george") && lower.contains("trophy")) return "Св. Георгий Победоносец";
            if (lower.contains("panteleimon")) return "Св. Пантелеимон Целитель";
            if (lower.contains("catherine")) return "Св. Екатерина Великомученица";
            if (lower.contains("theophany") || lower.contains("baptism")) return "Богоявление Господне";
            if (lower.contains("pascha")) return "Святая Пасха";
            if (lower.contains("dormition")) return "Успение Пресвятой Богородицы";
            if (lower.contains("nativity") && lower.contains("theotokos")) return "Рождество Пресвятой Богородицы";
        }

        return apiTitle;
    }
}