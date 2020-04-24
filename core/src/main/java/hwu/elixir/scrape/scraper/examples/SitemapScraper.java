package hwu.elixir.scrape.scraper.examples;

import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.parser.Parser;



import hwu.elixir.scrape.exceptions.CannotWriteException;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.MissingMarkupException;
import hwu.elixir.scrape.scraper.ScraperFilteredCore;


public class SitemapScraper extends ScraperFilteredCore {

    private static String outputFolder = System.getProperty("user.home");
    private static Logger logger = LoggerFactory.getLogger(System.class.getName());

    /**
     * Scrape a given URL and write to file in the home directory. Output will be in NQuads format.
     *
     * @param url The URL to scrape
     * @throws
     * @throws FourZeroFourException
     * @throws JsonLDInspectionException
     * @throws CannotWriteException
     */
    public void scrapeSitemap(String url, String outputFileName) {
        try {
            displayResult(url, scrape(url, outputFolder, outputFileName, 0L), outputFolder);
        } catch (FourZeroFourException | JsonLDInspectionException e) {
            logger.error("Cannot scrape site; error thrown", e);
        } catch (CannotWriteException e) {
            logger.error("Problem writing file for to the " + outputFolder + " directory.");
        } catch (MissingMarkupException e) {
            logger.error("Problem obtaining markup from " + url + ".");
        } finally {
            shutdown();
        }
    }

    public Elements getSitemapList(String url, String sitemapURLKey) throws IOException {

        Document doc = null;

        try {
             doc = Jsoup.connect(url).get();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        Elements elements = doc.select(sitemapURLKey);
        //Document doc = Jsoup.parse(html, "", Parser.xmlParser());

        return elements;
    }

    public static void main(String[] args) throws IOException {

        // Better way of setting the single url to scrape, DO NOT HARDCODE
        String urlToScrape = "https://www.ebi.ac.uk/biosamples/sitemap/29";

        urlToScrape = urlToScrape.toLowerCase();

        Elements sitemapList = null;
        boolean isSitemap = false;

        SitemapScraper scraperL = new SitemapScraper();

        // If a sitemap is passed as a parameter then somewhere in the URL the word sitemap will be present
        // but please note that this solution will ONLY work if sitemap is present on the url
        if (urlToScrape.indexOf("sitemap") != -1) {
            logger.info("URL is a sitemap");
            isSitemap = true;
        }

        if (isSitemap) {
           sitemapList = scraperL.getSitemapList(urlToScrape, "loc"); //urlset is pecific to EBI
            sitemapList.toArray();
        }

        /*for (int i = 0; i < sitemapList.size(); i++) {
             logger.info("sitemap element");
             System.out.println(sitemapList.get(i).text());
        }*/

        //http://www.ebi.ac.uk/biosamples/samples/SAMN02609793
        //https://www.ebi.ac.uk/biosamples/sitemap/29
        int count = 0;
        for (Element element : sitemapList) {

            if (count < 5 ) {
                logger.info("Scraping a URL: " + count);

                scraperL.scrapeSitemap(element.text(), "SitemapEBI_5elements");
                //scraperL.scrapeSitemap("http://www.ebi.ac.uk/biosamples/samples/SAMN02609793", "SitemapEBI29");
            }

            count++;
        }


    }
}