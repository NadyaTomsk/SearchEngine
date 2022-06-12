package main.service;

import main.dto.ResultData;
import main.dto.ResultSearchItem;
import main.model.PageEntity;
import main.repository.FieldRepository;
import main.repository.PageRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SearchService {

    private final PageRepository pageRepository;
    private final FieldRepository fieldRepository;

    @Autowired
    SearchService(PageRepository p, FieldRepository f) {
        pageRepository = p;
        fieldRepository = f;
        LemmaBuilder.getMorphService();
        LemmaBuilder.getEnglishMorphService();
    }

    public ResultData search(String searchString, List<String> siteUrl, int offset, int limit) {
        ResultData result = new ResultData();
        Set<ResultSearchItem> searchResults = new TreeSet<>();
        try {
            Map<String, Integer> lemmas = LemmaBuilder.lemmatization(searchString);
            lemmas.forEach((s, integer) -> {
                        if (s.length() > 255) {
                            lemmas.remove(s);
                        }
                    }
            );
            if (lemmas.size() == 0) {
                return result;
            }
            TreeMap<String, Integer> sortedLemma = sortLemma(lemmas);
            Page<PageEntity> pagesPage = pageRepository.getSearchResult(lemmas.keySet(), siteUrl, Pageable.ofSize(limit).withPage(offset / limit));
            result.setCountSearchResults(pagesPage.getTotalElements());
            List<PageEntity> pages = pagesPage.getContent();
            for (PageEntity page : pages) {
                Set<String> lemmaSet = sortedLemma.keySet();
                Document content = Jsoup.parse(page.getContent());
                String title = content.title();
                String snippet = SnippetBuilder.generateSnippet(content, lemmaSet, fieldRepository.findAll());
                StringJoiner wordsNotFound = new StringJoiner(", ");
                StringBuilder finalSnippet = new StringBuilder(snippet);
                lemmaSet.forEach(l -> {
                    if (!page.getLemmaList().contains(l)) {
                        wordsNotFound.add(l);
                    }
                });
                if (wordsNotFound.length() > 0) {
                    finalSnippet.append(System.lineSeparator())
                            .append("<p class = \"not-found-words\">Не найдено: <s>")
                            .append(wordsNotFound.toString())
                            .append("</s></p>");
                }
                ResultSearchItem s = new ResultSearchItem(page.getSite().getUrl(),
                        page.getSite().getName(),
                        page.getPath(),
                        title,
                        finalSnippet.toString(),
                        page.getPercentRank());
                searchResults.add(s);
            }
            result.setResult(true);
            result.setSearchResult(searchResults);
        } catch (Exception e) {
            result.setResult(false);
            result.setError("В процессе поиска возникла ошибка: " +
                    e.getMessage() +
                    System.lineSeparator() +
                    Arrays.toString(e.getStackTrace()));
        }
        return result;
    }

    private TreeMap<String, Integer> sortLemma(Map<String, Integer> lemmas) {
        ComparatorMapByValue comparator = new ComparatorMapByValue(lemmas);
        TreeMap<String, Integer> sortedLemma = new TreeMap<>(comparator);
        sortedLemma.putAll(lemmas);
        return sortedLemma;
    }

}
