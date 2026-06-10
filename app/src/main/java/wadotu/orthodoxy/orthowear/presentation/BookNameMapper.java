package wadotu.orthodoxy.orthowear.presentation;

import java.util.HashMap;
import java.util.Map;

public class BookNameMapper {

    private static final Map<String, String> map = new HashMap<>();
    private static final Map<String, Integer> canonicalIndexMap = new HashMap<>();

    // TXT 기반 canonical 약어 리스트 (정경 순서 그대로)
    private static final String[] CANONICAL_ORDER = {

            // 구약
            "Gen", "Exo", "Lev", "Num", "Deu",
            "Jos", "Jdg", "Rut",
            "1Sa", "2Sa",
            "1Ki", "2Ki",
            "1Ch", "2Ch",
            "Ezr", "Neh",
            "Tob", "Jdt", "Est",
            "1Ma", "2Ma",
            "Job", "Psa", "Pro", "Ecc", "Sol",
            "Wis", "Sir",
            "Isa", "Jer", "Lam", "Bar", "Eze", "Dan",
            "Hos", "Joe", "Amo", "Oba", "Jon", "Mic", "Nah", "Hab",
            "Zep", "Hag", "Zec", "Mal",

            // 신약
            "Mat", "Mar", "Luk", "Joh",
            "Act",
            "Rom",
            "1Co", "2Co",
            "Gal", "Eph", "Phi", "Col",
            "1Th", "2Th",
            "1Ti", "2Ti",
            "Tit", "Phm",
            "Heb",
            "Jam", "1Pe", "2Pe",
            "1Jo", "2Jo", "3Jo",
            "Jud",
            "Rev"
    };

    static {
        // ------------------------
        // 구약 (46권)
        // ------------------------

        map.put("Gen", "창세기");
        map.put("Exo", "출애굽기");
        map.put("Lev", "레위기");
        map.put("Num", "민수기");
        map.put("Deu", "신명기");
        map.put("Jos", "여호수아");
        map.put("Jdg", "판관기");
        map.put("Rut", "룻기");

        map.put("1Sa", "사무엘상");
        map.put("2Sa", "사무엘하");

        map.put("1Ki", "열왕기상");
        map.put("2Ki", "열왕기하");

        map.put("1Ch", "역대기상");
        map.put("2Ch", "역대기하");

        map.put("Ezr", "에즈라");
        map.put("Neh", "느헤미야");

        map.put("Tob", "토비트");
        map.put("Jdt", "유딧");
        map.put("Est", "에스델");

        map.put("1Ma", "마카베오상");
        map.put("2Ma", "마카베오하");

        map.put("Job", "욥기");
        map.put("Psa", "시편");
        map.put("Pro", "잠언");
        map.put("Ecc", "전도서");
        map.put("Sgs", "아가");

        map.put("Sol", "지혜서");
        map.put("Sir", "집회서");

        map.put("Isa", "이사야");
        map.put("Jer", "예레미야");
        map.put("Lam", "애가");
        map.put("Bar", "바룩");
        map.put("Eze", "에제키엘");
        map.put("Dan", "다니엘");

        map.put("Hos", "호세아");
        map.put("Joe", "요엘");
        map.put("Amo", "아모스");
        map.put("Oba", "오바드야");
        map.put("Jon", "요나");
        map.put("Mic", "미가");
        map.put("Nah", "나훔");
        map.put("Hab", "하바꾹");
        map.put("Zep", "스바니야");
        map.put("Hag", "하까이");
        map.put("Zec", "즈가리야");
        map.put("Mal", "말라기");

        // ------------------------
        // 신약 (27권)
        // ------------------------

        map.put("Mat", "마태오 복음경");
        map.put("Mar", "마르코 복음경");
        map.put("Luk", "루가 복음경");
        map.put("Joh", "요한 복음경");

        map.put("Act", "사도행전");

        map.put("Rom", "로마서");

        map.put("1Co", "I 고린토");
        map.put("2Co", "II 고린토");

        map.put("Gal", "갈라디아");
        map.put("Eph", "에페소");
        map.put("Phi", "필립비");
        map.put("Col", "골로사이");

        map.put("1Th", "I 데살로니카");
        map.put("2Th", "II 데살로니카");

        map.put("1Ti", "I 디모테오");
        map.put("2Ti", "II 디모테오");

        map.put("Tit", "디도");
        map.put("Phm", "필레몬");

        map.put("Heb", "히브리");

        map.put("Jam", "야고보서");
        map.put("1Pe", "I 베드로");
        map.put("2Pe", "II 베드로");

        map.put("1Jo", "I 요한");
        map.put("2Jo", "II 요한");
        map.put("3Jo", "III 요한");

        map.put("Jud", "유다");
        map.put("Rev", "요한 묵시록");

        // ==========
        // ★ canonicalIndexMap 초기화 — 반드시 필요
        // ==========
        for (int i = 0; i < CANONICAL_ORDER.length; i++) {
            canonicalIndexMap.put(CANONICAL_ORDER[i], i);
        }
    }

    // BookNameMapper 클래스 안에 추가
// (static 블록 아래, get() 메서드 위 또는 아래 아무 곳에나 public static으로 추가)
    private static final Map<String, String> reverseMap = new HashMap<>();

    static {
        // 기존 static 초기화 끝난 직후(또는 canonicalIndexMap 초기화 바로 다음)에 reverseMap 채우기
        for (Map.Entry<String, String> e : map.entrySet()) {
            reverseMap.put(e.getValue(), e.getKey()); // e.g. "창세기" -> "Gen"
        }
    }

    /**
     * displayName(예: "창세기")으로 canonical(예: "Gen")을 반환.
     * 알 수 없으면 null 반환.
     */
    public static String getCanonicalFromDisplay(String displayName) {
        if (displayName == null) return null;
        String c = reverseMap.get(displayName);
        if (c != null) return c;
        // 추가 시도: 공백/점/콤마 제거한 형태로 시도
        String normalized = displayName.replaceAll("[\\s\\.,]","").trim();
        return reverseMap.getOrDefault(normalized, null);
    }

    /**
     * displayName(예: "창세기")으로 정경인덱스 가져오기. 알 수 없으면 MAX 반환.
     */
    public static int getCanonicalIndexFromDisplay(String displayName) {
        String canon = getCanonicalFromDisplay(displayName);
        if (canon == null) return Integer.MAX_VALUE;
        return getCanonicalIndex(canon);
    }

    public static String get(String key) {
        return map.getOrDefault(key, key);
    }

    // 정경 순서 index 반환
    public static int getCanonicalIndex(String canonical) {
        if (canonical == null) return Integer.MAX_VALUE;
        return canonicalIndexMap.getOrDefault(canonical, Integer.MAX_VALUE);
    }
}
