package main.service;

import main.dto.ResultData;
import main.dto.ResultStatisticsItem;
import main.model.SiteEntity;
import main.repository.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticService {

    private static final Logger LOGGER = LogManager.getLogger(StatisticService.class);

    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private SiteRepository siteRepository;

    @Autowired
    public StatisticService(SiteRepository s, PageRepository p, LemmaRepository l) {
        siteRepository = s;
        pageRepository = p;
        lemmaRepository = l;
    }

    public ResultData getStatistics() {
        ResultData result = new ResultData();
        result.setResult(false);
        try {
            ResultStatisticsItem statistics = new ResultStatisticsItem();
            statistics.setTotal(getTotalStatistics());
            statistics.setDetailed(getDetailedStatistics());
            result.setStatistics(statistics);
            result.setResult(true);
        } catch (Exception e) {
            StringBuilder error = new StringBuilder();
            error.append("Ошибка БД при сборе статистики!").append(System.lineSeparator()).append(e.toString());
            LOGGER.error(error.toString());
            result.setError(error.toString());
        }
        return result;
    }

    private List<ResultStatisticsItem.Detailed> getDetailedStatistics() throws Exception {
        List<ResultStatisticsItem.Detailed> result = new ArrayList<>();
        List<SiteEntity> sites = siteRepository.findAll();
        if (sites.isEmpty()) {
            return result;
        }
        sites.forEach(siteEntity -> {
            ResultStatisticsItem.Detailed site = new ResultStatisticsItem.Detailed();
            site.setUrl(siteEntity.getUrl());
            site.setName(siteEntity.getName());
            site.setStatus(siteEntity.getStatus().name());
            site.setStatusTime(System.currentTimeMillis());
            site.setError(siteEntity.getLastError());
            site.setPages(siteRepository.getPagesCountById(siteEntity.getId()));
            site.setLemmas(siteRepository.getLemmasCountById(siteEntity.getId()));
            result.add(site);
        });
        return result;
    }

    private ResultStatisticsItem.Total getTotalStatistics() throws Exception {
        ResultStatisticsItem.Total result = new ResultStatisticsItem.Total();
        result.setSites(siteRepository.count());
        result.setPages(pageRepository.count());
        result.setLemmas(lemmaRepository.count());
        result.setIndexing(siteRepository.isIndexing() == 1);
        return result;
    }

}
