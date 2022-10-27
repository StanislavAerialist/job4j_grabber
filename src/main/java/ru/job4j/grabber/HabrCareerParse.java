package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse implements Parse {
    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer?page=", SOURCE_LINK);
    private static final int PAGE_COUNT = 5;
    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    private static String retrieveDescription(String link) {
        Connection connection = Jsoup.connect(link);
        Document document = null;
        try {
            document = connection.get();
        } catch (IOException e) {
            throw new IllegalArgumentException("Connection error");
        }
        Elements rows = document.select(".style-ugc");
        return rows.text();
    }

    private Post parsePost(Element row) {
            Element titleElement = row.select(".vacancy-card__title").first();
            Element linkElement = titleElement.child(0);
            String vacancyName = titleElement.text();
            Element dateElement = row.select(".vacancy-card__date").first();
            Element date = dateElement.child(0);
            String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
            String description = retrieveDescription(link);
            String dateS = date.attr("datetime");
        return new Post(vacancyName, link, description, dateTimeParser.parse(dateS));
    }

    @Override
    public List<Post> list(String page) {
        List<Post> rsl = new ArrayList<>();
        for (int i = 1; i <= PAGE_COUNT; i++) {
            Connection connection = Jsoup.connect(page + i);
            Document document = null;
            try {
                document = connection.get();
            } catch (IOException e) {
                throw new IllegalArgumentException("Parsing error");
            }
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> rsl.add(parsePost(row)));
            }
        return rsl;
    }
}
