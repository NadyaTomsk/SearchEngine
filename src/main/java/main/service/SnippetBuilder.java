package main.service;

import lombok.extern.log4j.Log4j2;
import main.model.FieldEntity;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Log4j2
public class SnippetBuilder {
    private static final String TAGS_SCRIPT = "(<script.*>).*|\\s*(</script>)";
    private static final String REG_ALL_TAGS = "/</?[\\w\\s]*>|<.+[\\W]>/g";//"<!?\\/?[a-z\\s\"0-9=_]*>";
    private static final int SNIPPET_INTERVAL = 5;
    private static final int SNIPPET_MAX_LENGTH = 200;

    public static String generateSnippet(Document content, Set<String> lemmas, Iterable<FieldEntity> fields) {
        StringBuilder snippetBuilder = new StringBuilder();
        Pattern pattern_script = Pattern.compile(TAGS_SCRIPT);
        for (FieldEntity field : fields) {
            Element element = content.select(field.getSelector()).first();
            String text = element.text();
            String contentStr = text;
            Matcher matcher = pattern_script.matcher(text);
            while (matcher.find()) {
                int start_script = matcher.start(1);
                int end_script = Math.max(matcher.end(2), matcher.end(1));
                text = text.replaceAll(contentStr.substring(start_script, end_script), "");
            }
            text = text.replaceAll(REG_ALL_TAGS, "");
            String nextOneFieldSnippet = snippetForField(text, lemmas);
            if (snippetBuilder.length() > 0 && nextOneFieldSnippet.length() > 0) {
                snippetBuilder.append("...");
            }
            snippetBuilder.append(nextOneFieldSnippet);
        }
        return snippetBuilder.toString();
    }

    private static String snippetForField(String text, Set<String> lemmas) {
        StringBuilder snippetBuilder = new StringBuilder();
        String[] words = text.trim().split("\\s+");
        Set<String> wordsFound = new TreeSet<>();
        for (int i = 0; i < words.length; i++) {
            String word = words[i].trim();
            String lemmaWord = LemmaBuilder.getLemma(word);
            if (word.length() == 0 || lemmaWord == null) {
                continue;
            }
            if (lemmas.stream().toList().contains(lemmaWord)) {
                wordsFound.add(word.replaceAll("[^a-zA-Zа-яА-ЯёЁ]", ""));
                int start = Math.max(i - SNIPPET_INTERVAL, 0);
                int end = Math.min(i + SNIPPET_INTERVAL, words.length);
                int totalLength = snippetBuilder.length() + List.of(words).subList(start, end).stream().flatMapToInt(s -> IntStream.of(s.length())).sum();
                if (totalLength >= SNIPPET_INTERVAL) {
                    end = i;
                }
                snippetBuilder.append(start > 0 ? "..." : "");
                List.of(words).subList(start, end).forEach(w -> {
                    snippetBuilder.append(w).append(" ");
                });
                if (snippetBuilder.length() > SNIPPET_MAX_LENGTH) {
                    break;
                }
                snippetBuilder.append(end < words.length ? "..." : "");
            }
        }

        String snippet = snippetBuilder.toString();
        if (snippet.length() > 0) {
            for (String word : wordsFound) {
                snippet = snippet.replaceAll(word, "<b>" + word + "</b>");
            }

        }
        return snippet;
    }

}
