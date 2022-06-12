package main.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LemmaBuilder {

    private static final Logger LOGGER = LogManager.getLogger(LemmaBuilder.class);
    private static final String[] NOT_ANALYZE = {"СОЮЗ", "МЕЖД", "ПРЕДЛ", "ЧАСТ"};
    private static final String[] NOT_ANALYZE_ENG = {"PN", "PREP", "PART", "ARTICLE"};
    private static final String REG_NOT_SYMBOLS = "[^а-яё\\s]";
    private static final String REG_NOT_SYMBOLS_ENG = "[^a-z\\s]";
    private static LuceneMorphology luceneMorph;
    private static EnglishLuceneMorphology englishMorph;

    public static LuceneMorphology getMorphService() {
        if (luceneMorph == null) {
            try {
                luceneMorph = new RussianLuceneMorphology();
            } catch (Exception e) {
                LOGGER.error("Ошибка инициализации лемматизатора(русский)!{}{}{}{}",
                        System.lineSeparator(),
                        e.getMessage(),
                        System.lineSeparator(),
                        e.getStackTrace());
            }
        }
        return luceneMorph;
    }

    public static EnglishLuceneMorphology getEnglishMorphService() {
        if (englishMorph == null) {
            try {
                englishMorph = new EnglishLuceneMorphology();
            } catch (Exception e) {
                LOGGER.error("Ошибка инициализации лемматизатора(англ)!{}{}{}{}",
                        System.lineSeparator(),
                        e.getMessage(),
                        System.lineSeparator(),
                        e.getStackTrace());
            }
        }
        return englishMorph;
    }

    public static Map<String, Integer> lemmatization(String text) {
        Map<String, Integer> words = new HashMap<>();
        if (text == null || text.length() == 0) {
            return words;
        }
        String[] list = text.toLowerCase(Locale.ROOT)
                .replaceAll(REG_NOT_SYMBOLS, " ").trim().split("\\s+");
        LuceneMorphology service = getMorphService();
        for (String word : list) {
            word = word.trim();
            if (word.length() < 2) {
                continue;
            }
            String[] morph = service.getMorphInfo(word).get(0).split("\\s");
            if (morph.length > 1 && isWordToLemma(morph[1])) {
                String normalForm = service.getNormalForms(word).get(0);
                int count = words.getOrDefault(normalForm, 0) + 1;
                words.put(normalForm.replaceAll("ё", "е"), count);
            }
        }
        EnglishLuceneMorphology english = getEnglishMorphService();
        String[] englishList = text.toLowerCase(Locale.ROOT).replaceAll(REG_NOT_SYMBOLS_ENG, " ").trim().split("\\s+");
        for (String word : englishList) {
            word = word.trim();
            if (word.length() < 2) {
                continue;
            }
            String[] morph = english.getMorphInfo(word).get(0).split("\\s");
            if (morph.length > 1 && isWordToLemmaEng(morph[1])) {
                String normalForm = english.getNormalForms(word).get(0);
                int count = words.getOrDefault(normalForm, 0) + 1;
                words.put(normalForm, count);
            }
        }
        return words;
    }

    public static String getLemma(String word) {
        String rusWord = word.toLowerCase(Locale.ROOT).replaceAll(REG_NOT_SYMBOLS, "").trim();
        if (rusWord.length() > 0) {
            LuceneMorphology service = getMorphService();
            return service.getNormalForms(rusWord).get(0);
        } else {
            String engWord = word.toLowerCase(Locale.ROOT).replaceAll(REG_NOT_SYMBOLS_ENG, "").trim();
            if (engWord.length() > 0) {
                EnglishLuceneMorphology english = getEnglishMorphService();
                return english.getNormalForms(engWord).get(0);
            }
            return null;
        }
    }

    private static boolean isWordToLemma(String word) {
        return Arrays.stream(NOT_ANALYZE).noneMatch(s -> s.equals(word));
    }

    private static boolean isWordToLemmaEng(String word) {
        return Arrays.stream(NOT_ANALYZE_ENG).noneMatch(s -> s.equals(word));
    }
}
