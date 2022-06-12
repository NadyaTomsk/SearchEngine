package main.service;

import lombok.Getter;
import main.dto.PageOfSite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SiteMapCreator extends RecursiveAction {

    private static final Logger LOGGER = LogManager.getLogger(SiteMapCreator.class);
    private static final int MIN_SLEEP_TIME = 500;
    private static final int MAX_SLEEP_TIME = 5000;

    private static volatile long LAST_GET_CONTENT = 0;


    @Getter
    private final URL url;
    private final int siteId;
    private final IndexService service;
    private final List<SiteMapCreator> list = new ArrayList<>();

    public SiteMapCreator(URL url, int siteId, IndexService service) {
        this.url = url;
        this.siteId = siteId;
        this.service = service;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        list.forEach(t -> t.cancel(true));
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    protected void compute() {
        if (isCancelled()) {
            return;
        }
        Set<String> childes = getChildes();

        for (String child : childes) {
            if (isCancelled()) {
                return;
            }
            try {
                URL childURL = new URL(child);
                SiteMapCreator creator = new SiteMapCreator(childURL, siteId, service);
                list.add(creator);
            } catch (Exception e) {
                String error = String.format("Ошибка выполнения рекурсивных задач - %s!\n%s", child, e.getMessage());
                LOGGER.error("{}\n{}", error, e.getStackTrace());
                service.setLastError(error);
            }
        }
        invokeAll(list);
    }

    private Set<String> getChildes() {
        HashSet<String> childes = new HashSet<>();
        if (isCancelled()) {
            return childes;
        }
        try {
            PageOfSite node = getContent(url, siteId, service.getUserAgent());
            service.addPage(node);
            if (node.getContent() == null || node.getContent().length() == 0) {
                service.setLastError(node.getError());
                return childes;
            }
            Document doc = Jsoup.parse(node.getContent());
            Elements elements = doc.select("a[href]");
            for (Element element : elements) {
                String attr = element.attr("href");
                attr = attr.endsWith("/") ? attr.substring(0, attr.length() - 1) : attr;
                childListAdd(childes, attr);
            }
        } catch (Exception e) {
            String error = String.format("Ошибка обработки контента страницы - %s!\n%s", url, e.getMessage());
            LOGGER.error("{}\n{}", error, e.getStackTrace());
            service.setLastError(error);
        }
        return childes;
    }

    private boolean checkNotChild(String child) {
        return (child == null
                || child.length() == 0
                || child.matches(".*#$")
                || child.contains("javascript")
                || child.startsWith("tel")
                || child.startsWith("foto")
                || child.startsWith("tg")
                || child.equals(url.toString())
                || child.equals(url + "/")
                || (child + "/").equals(url.getPath())
                || (child + "/").equals(url.toString())
                || (!child.startsWith(url.getPath())) && !child.startsWith(url.toString()));

    }

    private void childListAdd(Set<String> childes, String child) {
        if (checkNotChild(child)) {
            return;
        }
        if (child.startsWith("/")) {
            String startPath = url.toString();
            if (url.getPath().length() > 1) {
                startPath = startPath.replace(url.getPath(), "");
            }
            startPath = startPath.endsWith("/") ? startPath.substring(0, url.toString().length() - 1) : startPath;
            child = startPath + child;
        }
        Optional<String> isChild = childes.stream().filter(child::contains).findFirst();
        if (isChild.isEmpty()) {
            try {
                URL childURL = new URL(child);
                if (!childURL.getHost().replace("www\\.", "").equals(url.getHost())
                        || childURL.getPath().equals(url.getPath())) {
                    return;
                }
            } catch (Exception e) {
                String error = String.format("Ошибка парсинга URL - %s!\n%s", child, e.getMessage());
                LOGGER.error("{}\n{}", error, e.getStackTrace());
                service.setLastError(error);
                return;
            }
            if (!childes.isEmpty()) {
                String finalChild = child;
                childes.removeIf(c -> c.contains(finalChild));
            }
            childes.add(child);
        }
    }

    public static PageOfSite getContent(URL url, int siteId, String userAgent) {
        if (LAST_GET_CONTENT == 0) {
            LAST_GET_CONTENT = System.currentTimeMillis();
        }
        LOGGER.info("Получаем инфу со страницы {}{} siteId - {}", url.toString(), System.lineSeparator(), siteId);
        Connection connection = Jsoup.connect(url.toString())
                .ignoreHttpErrors(true)
                .userAgent(userAgent)
                .referrer("http://www.google.com")
                .maxBodySize(0);
        Document doc = null;
        int code = 0;
        String error = "";
        try {
            long timeToSleep = (long) (Math.random() * MIN_SLEEP_TIME + MAX_SLEEP_TIME);
            long timeSinceLastGet = System.currentTimeMillis() - LAST_GET_CONTENT;
            long sleep = Math.max(0, timeToSleep - timeSinceLastGet);
            Thread.sleep(sleep);
            Connection.Response response = connection.execute();
            LAST_GET_CONTENT = System.currentTimeMillis();
            if (response.contentType() == null || (!response.contentType().contains("text/html")
                    && !response.contentType().contains("xml"))) {
                error = "Wrong content type";
            } else {
                doc = response.parse();
            }
            code = response.statusCode();
        } catch (Exception e) {
            Pattern pattern = Pattern.compile("Status=([0-9]*)");
            Matcher matcher = pattern.matcher(e.getMessage());
            if (matcher.matches() && matcher.groupCount() > 0) {
                code = Integer.parseInt(matcher.group(1));
            }
            error = String.format("Ошибка запроса данных по URL - %s!\n%s", url, e.getMessage());
            LOGGER.error("{}\n{}", error, e.getStackTrace());
        }

        PageOfSite node = new PageOfSite();
        node.setContent(doc == null ? "" : doc.outerHtml());
        node.setPath(url.getPath());
        node.setCode(code);
        node.setSiteId(siteId);
        node.setError(error);
        return node;
    }

}

