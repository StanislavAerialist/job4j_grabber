package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;

public class HabrCareerParse {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer?page=", SOURCE_LINK);

    public static void show(String page, int i) throws IOException {
        Connection connection = Jsoup.connect(page + i);
        Document document = connection.get();
        Elements rows = document.select(".vacancy-card__inner");
        rows.forEach(row -> {
            Element titleElement = row.select(".vacancy-card__title").first();
            Element linkElement = titleElement.child(0);
            String vacancyName = titleElement.text();
            Element dateElement = row.select(".vacancy-card__date").first();
            Element date = dateElement.child(0);
            String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
            String dateS = date.attr("datetime");
            System.out.printf("%s %s %s%n", vacancyName, link, dateS);
        });
    }

    public static void main(String[] args) throws IOException {
        for (int i = 1; i <= 5; i++) {
            show(PAGE_LINK, i);
        }
    }
}
