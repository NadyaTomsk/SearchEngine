package main.controller;

import main.dto.ResultData;
import main.dto.ResultError;
import main.dto.ResultSearch;
import main.dto.ResultStatistics;
import main.model.StatusType;
import main.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@EnableConfigurationProperties(value = ConfigurationApplication.class)
@RestController
public class SearchEngineController {

    private static final String RESULT_TRUE = "\"result\":true";
    private static final Logger LOGGER = LogManager.getLogger(SearchEngineController.class);
    @Autowired
    private ConfigurationApplication config;

    @Autowired
    private IndexService service;
    @Autowired
    private SearchService searchService;
    @Autowired
    private StatisticService statisticService;

    private Future<ResultData> indexingThread;

    @GetMapping("/admin/statistics")
    public ResponseEntity<?> getStatistics() {
        service.setUserAgent(config.getUserAgent());
        ResultData result = statisticService.getStatistics();
        LOGGER.info("Вывод статистики {}", result.getStatistics().getTotal().toString());
        if (result.isResult()) {
            ResultStatistics stat = new ResultStatistics(result);
            return ResponseEntity.ok(stat);
        } else {
            ResultError error = new ResultError(result);
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/admin/indexPage")
    public ResponseEntity indexOnePage(@RequestParam String url) {
        ResultData result;
        ConfigurationApplication.Site site = new ConfigurationApplication.Site();
        site.setUrl(url);
        site.setName("");
        indexingThread = null;
        service.setCanceledCreateIndex(false);
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        indexingThread = pool.submit(() -> service.indexOnePage(url));
        Future<ResultData> index = indexingThread;
        try {
            result = index.get();
            if (result.isResult()) {
                return ResponseEntity.ok(RESULT_TRUE);
            }
            ResultError error = new ResultError(result);
            return ResponseEntity.internalServerError().body(error);
        } catch (Exception e) {
            ResultError error = new ResultError("Ошибка" + System.lineSeparator() + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/admin/stopIndexing")
    public ResponseEntity<?> getStopIndexing() {
        if (getIndexingThread(config.sites).isDone()) {
            ResultError error = new ResultError("Индексация завершена!");
            return ResponseEntity.badRequest().body(error);
        }
        if (getIndexingThread(config.sites).cancel(true)) {
            service.stopIndexing();
            return ResponseEntity.ok(RESULT_TRUE);
        }
        ResultError error = new ResultError("Индексация не запущена, либо не может быть завершена!");
        return ResponseEntity.badRequest().body(error);
    }

    @GetMapping("/admin/search")
    public ResponseEntity<?> getSearchResult(@RequestParam(required = false) String site,
                                             @RequestParam String query,
                                             @RequestParam(defaultValue = "0") int offset,
                                             @RequestParam(defaultValue = "20") int limit) {
        if (!query.isEmpty()) {
            try {
                ResultSearch search = new ResultSearch();
                ResultData resultData = search(site, query, offset, limit);
                if (resultData.isResult()) {
                    search.setResult(true);
                    search.setData(resultData.getSearchResult());
                    search.setCount(resultData.getCountSearchResults());
                    return ResponseEntity.ok(search);
                }
                return ResponseEntity.badRequest().body(new ResultError(resultData));
            } catch (Exception e) {
                ResultError error = new ResultError("Ошибка поиска!" + System.lineSeparator() + e.getMessage());
                return ResponseEntity.internalServerError().body(error);
            }
        }
        return ResponseEntity.badRequest().body(new ResultError("Пустая строка поиска!"));
    }

    @GetMapping("/admin/startIndexing")
    public ResponseEntity<?> getChildes(Model model) {
        if (indexingThread != null && !indexingThread.isDone()) {
            return ResponseEntity.badRequest().body(new ResultError("Индексация уже запущена"));
        }
        service.setCanceledCreateIndex(false);
        ResultData result = new ResultData();
        try {
            indexingThread = null;
            service.setUserAgent(config.getUserAgent());
            service.deleteAll(config.sites);
            result = getIndexingThread(config.sites).get();
            if (result.isResult()) {
                return ResponseEntity.ok(RESULT_TRUE);
            }
            ResultError error = new ResultError(result);
            return ResponseEntity.internalServerError().body(error);
        } catch (Exception e) {
            ResultError error = new ResultError("Ошибка URL!" + System.lineSeparator() + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    private ResultData search(String site, String query, int offset, int limit) {
        ResultData resultData = new ResultData();
        resultData.setResult(true);
        List<ConfigurationApplication.Site> sites = config.getSites();
        if (site != null) {
            sites = sites.stream().filter(s -> s.getUrl().equals(site)).toList();
        }
        resultData = searchService.search(query,
                sites.stream().map(ConfigurationApplication.Site::getUrl).toList(),
                offset,
                limit);
        return resultData;
    }

    private Future<ResultData> getIndexingThread(List<ConfigurationApplication.Site> sites) {
        if (indexingThread == null) {
            ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            indexingThread = pool.submit(() -> {
                ResultData result = new ResultData();
                for (ConfigurationApplication.Site site : sites) {
                    try {
                        result = service.indexPage(site);
                    } catch (Exception e) {
                        result.setResult(false);
                        result.setError("Ошибка!" + System.lineSeparator() + e.getMessage());
                        LOGGER.error(result.getError());
                        service.setSiteStatus(site.getUrl(), StatusType.FAILED, result.getError());
                    }
                }
                return result;
            });
        }
        return indexingThread;
    }

}
