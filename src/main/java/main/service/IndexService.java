package main.service;

import lombok.Getter;
import lombok.Setter;
import main.controller.ConfigurationApplication;
import main.dto.PageOfSite;
import main.dto.ResultData;
import main.model.*;
import main.repository.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Service
public class IndexService {

    private static final Logger LOGGER = LogManager.getLogger(IndexService.class);
    private static final String REG_ALL_TAGS = "<!?\\/?[a-z\\s\"0-9=_]*>";
    public static final String TEXT_STOP_INDEX_ERROR = "Индексация прервана принудительно.";
    @Getter
    @Setter
    private String userAgent;
    @Getter
    @Setter
    private volatile boolean isCanceledCreateIndex = false;
    @Getter
    @Setter
    private volatile String lastError;

    private SiteMapCreator mapCreator;
    private final PageRepository pageRepository;
    private final FieldRepository fieldRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;

    @Autowired
    IndexService(IndexRepository i, PageRepository p, FieldRepository f, LemmaRepository l, SiteRepository s) {
        indexRepository = i;
        pageRepository = p;
        fieldRepository = f;
        lemmaRepository = l;
        siteRepository = s;
        LemmaBuilder.getMorphService();
        LemmaBuilder.getEnglishMorphService();
        Optional<FieldEntity> optionalBody = fieldRepository.findByName("body");
        if (optionalBody.isEmpty()) {
            FieldEntity body = new FieldEntity();
            body.setName("body");
            body.setSelector("body");
            body.setWeight(0.8F);
            fieldRepository.save(body);
        }
        Optional<FieldEntity> optionalTitle = fieldRepository.findByName("title");
        if (optionalTitle.isEmpty()) {
            FieldEntity title = new FieldEntity();
            title.setName("title");
            title.setSelector("title");
            title.setWeight(1.0F);
            fieldRepository.save(title);
        }

    }

    public SiteEntity getSiteID(String siteUrl, String siteName) {
        Optional<SiteEntity> siteOpt = siteRepository.findByUrl(siteUrl);
        if (siteOpt.isEmpty()) {
            SiteEntity siteEntity = new SiteEntity();
            siteEntity.setUrl(siteUrl);
            siteEntity.setName(siteName);
            siteEntity.setStatus(StatusType.INDEXING);
            siteEntity.setStatusTime(System.currentTimeMillis());
            return siteRepository.save(siteEntity);
        }
        return siteOpt.get();
    }

    public void addPage(PageOfSite page) {
        if (isCanceledCreateIndex) {
            return;
        }
        pageRepository.flush();
        PageEntity entity = getEntityFromPage(page);
        try {
            if (entity == null) {
                entity = new PageEntity();
                entity.setPath(page.getPath());
                entity.setCode(page.getCode());
                entity.setContent(page.getContent());
                Optional<SiteEntity> site = siteRepository.findById(page.getSiteId());
                entity.setSite(site.get());
                pageRepository.saveAndUpdate(entity);
            } else if (!entity.getContent().equals(page.getContent())) {
                entity.setContent(page.getContent());
                entity.setCode(page.getCode());
                pageRepository.saveAndUpdate(entity);
            }
        } catch (Exception e) {
            String error = "Ошибка сохранения страницы - [" + page.getId() + "] -" +
                    page.getPath() + System.lineSeparator() + e.getMessage();
            LOGGER.error("{}\n{}", error, e.getStackTrace());
            lastError = error;
            setSiteStatus(entity.getSite(), StatusType.FAILED, error);
        }
        createIndex(entity);
        setSiteStatus(entity.getSite(), StatusType.INDEXING, lastError);
    }

    public List<PageOfSite> getAllPages() {
        Iterable<PageEntity> iterablePages = pageRepository.findAll();
        List<PageOfSite> pages = new ArrayList<>();
        for (PageEntity pageEntity : iterablePages) {
            pages.add(getPageFromEntity(pageEntity));
        }
        return pages;
    }

    public void stopIndexing() {
        LOGGER.info("Процесс построения индекса прерван!");
        isCanceledCreateIndex = true;
        if (mapCreator == null || mapCreator.isCancelled()) {
            return;
        }
        mapCreator.cancel(true);
    }

    public ResultData indexPage(ConfigurationApplication.Site siteConfig) {
        ResultData result = new ResultData();
        lastError = null;
        SiteEntity site = getSiteID(siteConfig.getUrl(), siteConfig.getName());
        if (isCanceledCreateIndex) {
            result.setResult(false);
            result.setError(TEXT_STOP_INDEX_ERROR);
            setSiteStatus(site, StatusType.FAILED, TEXT_STOP_INDEX_ERROR);
            return result;
        }
        try {
            setSiteStatus(site, StatusType.INDEXING, null);
            indexRepository.deleteByUrl(siteConfig.getUrl());
            lemmaRepository.deleteByUrl(siteConfig.getUrl());
            pageRepository.deleteByUrl(siteConfig.getUrl());
            mapCreator = new SiteMapCreator(new URL(siteConfig.getUrl()), site.getId(), this);
            ForkJoinPool fjp = new ForkJoinPool();
            fjp.invoke(mapCreator);
            LOGGER.info("Result {}", result);
            if (lastError == null || lastError.length() == 0) {
                setSiteStatus(site, StatusType.INDEXED, null);
                result.setResult(true);
            } else {
                setSiteStatus(site, StatusType.FAILED, lastError);
                result.setResult(false);
            }
        } catch (Exception e) {
            if (isCanceledCreateIndex) {
                result.setResult(false);
                result.setError(TEXT_STOP_INDEX_ERROR);
                setSiteStatus(site, StatusType.FAILED, TEXT_STOP_INDEX_ERROR);
                return result;
            } else {
                StringBuilder error = new StringBuilder();
                error.append("Ошибка индексирования! сайт - ").append(site.getUrl())
                        .append(System.lineSeparator()).append(e.getMessage());
                LOGGER.error("{}\n{}", error.toString(), e.getStackTrace());
                setSiteStatus(site, StatusType.FAILED, error.toString());
                result.setResult(false);
                result.setError(error.toString());
            }
        }
        return result;
    }


    public PageEntity getEntityFromPage(PageOfSite page) {
        if (page == null) {
            return null;
        }
        Optional<PageEntity> optionalPage;
        if (page.getId() > 0) {
            optionalPage = pageRepository.findById(page.getId());
        } else {
            optionalPage = pageRepository.findByPath(page.getPath(), page.getSiteId());
        }
        if (optionalPage.isEmpty()) {
            return null;
        }
        return optionalPage.get();
    }

    public PageOfSite getPageFromEntity(PageEntity pageEntity) {
        return new PageOfSite(pageEntity.getId(),
                pageEntity.getPath(),
                pageEntity.getContent(),
                pageEntity.getCode(),
                pageEntity.getSite().getId(),
                null);
    }

    public void createIndex(PageEntity page) {
        lemmaRepository.flush();
        indexRepository.flush();
        if (page == null || page.getContent() == null || isCanceledCreateIndex) {
            return;
        }
        Document content = Jsoup.parse(page.getContent());
        Iterable<FieldEntity> fields = fieldRepository.findAll();
        for (FieldEntity field : fields) {
            Elements elements = content.select(field.getSelector());
            for (Element element : elements) {
                String text = element.text();
                addLemma(field, page, text);
            }
        }
    }

    public void addLemma(FieldEntity field, PageEntity page, String text) {
        lemmaRepository.flush();
        indexRepository.flush();
        text = text.replaceAll(REG_ALL_TAGS, "");
        try {
            Map<String, Integer> lemmas = LemmaBuilder.lemmatization(text);
            TreeSet<IndexEntity> indexes = new TreeSet<>();
            TreeSet<LemmaEntity> lemmaEntities = new TreeSet<>();
            for (Map.Entry<String, Integer> lemmaMap : lemmas.entrySet()) {
                if (lemmaMap.getKey().length() > 255) {
                    continue;
                }
                LemmaEntity lemma;
                Optional<LemmaEntity> lemmaEntityOptional =
                        lemmaRepository.findByLemmaSite(lemmaMap.getKey(), page.getSite().getId());
                int freq = lemmaMap.getValue();
                if (lemmaEntityOptional.isEmpty()) {
                    lemma = new LemmaEntity(lemmaMap.getKey(), page.getSite());
                } else {
                    lemma = lemmaEntityOptional.get();
                    freq += lemma.getFrequency();
                }
                lemma.setFrequency(freq);
                //lemma = lemmaRepository.saveAndFlush(lemma);
                lemmaEntities.add(lemma);
            }
            lemmaRepository.saveAllAndFlush(lemmaEntities);
            for (LemmaEntity lemma : lemmaEntities) {
                float rank = lemmas.get(lemma.getLemma()) * field.getWeight();
                IndexEntity indexEntity = new IndexEntity();
                indexEntity.setLemma(lemma);
                indexEntity.setPage(page);
                indexEntity.setRank(rank);
                indexes.add(indexEntity);
            }
            indexRepository.saveAll(indexes);
        } catch (Exception e) {
            lastError = String.format("Ошибка при создании индексов! параметры :%s,%s\n%s", field, page, e.getMessage());
            LOGGER.error("{}\n{}", lastError, e.getStackTrace());
        }
    }

    public ResultData indexOnePage(String pageURL) {
        ResultData resultData = new ResultData();
        try {
            URL url = new URL(pageURL);
            Optional<SiteEntity> siteOptional = siteRepository.findAll().stream().filter(s -> s.getUrl().contains(url.getHost())).findFirst();
            if (siteOptional.isEmpty()) {
                resultData.setResult(false);
                resultData.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
                return resultData;
            }
            SiteEntity site = siteOptional.get();
            PageOfSite page = SiteMapCreator.getContent(url, site.getId(), userAgent);
            if (page.getContent() == null || page.getContent().length() == 0) {
                resultData.setResult(false);
                resultData.setError("Не удалось загрузить контент страницы " + pageURL);
                return resultData;
            }
            addPage(page);
            resultData.setResult(true);

        } catch (Exception e) {
            StringBuilder error = new StringBuilder();
            error.append("Ошибка индексирования страницы! сайт - ").append(pageURL)
                    .append(System.lineSeparator()).append(e.toString());
            LOGGER.error(error.toString());
            resultData.setResult(false);
            resultData.setError(error.toString());
        }
        return resultData;
    }

    public boolean setSiteStatus(String url, StatusType status, @Nullable String error) {
        Optional<SiteEntity> siteOpt = siteRepository.findByUrl(url);
        if (siteOpt.isEmpty()) {
            return false;
        }
        return setSiteStatus(siteOpt.get(), status, error);
    }

    public boolean setSiteStatus(SiteEntity site, StatusType status, @Nullable String error) {
        site.setStatus(status);
        site.setStatusTime(System.currentTimeMillis());
        site.setLastError(error == null ? "" : error);
        siteRepository.save(site);
        return true;
    }

    public void deleteAll(List<ConfigurationApplication.Site> sites) {
        List<String> urls = sites.stream().map(ConfigurationApplication.Site::getUrl).collect(Collectors.toList());
        indexRepository.deleteAllNotIn(urls);
        lemmaRepository.deleteAllNotIn(urls);
        pageRepository.deleteAllNotIn(urls);
        siteRepository.deleteAllNotIn(urls);
    }
}
