# Scraper

## Description

Reads URLs from a mysql database, scrapes the JSON-LD or RDFa from that page and produces quads which are saved to a file.

Currently there is no automatic process to load the quads into a triplestore.

Browsing the resulting triplestore is done through [Bischemas Knowledge Graph Explorer](https://github.com/HW-SWeL/BSKgE).

By default [JSoup](https://jsoup.org/) is used to retrieve HTML. If this fails [HtmlExtractor](https://github.com/HW-SWeL/HtmlExtractor) is used as a fallback.

### Design decisions

* Virtuoso was selected as the triplestore because it can handle very large numbers of triples with the free version.

* Quads are not automatically loaded into the triplestore as that is very slow, and scraping is slow enough. Saving to file then bulk importing is much quicker.


## Build instructions

Requirements:
* maven v3.6.0
* mysql v8.0.13
* other requirements are provided through pom file

### Instructions for running

1. Insert URLs into mysql. [Example SQL file](https://gist.github.com/kcmcleod/202c0687792ce7ec56b68bbfd6900a83) (based on the live deploy list) that can be imported to get started. You will need a user that has insert, select, delete, update, drop, create & alter privileges.
2. Add details of your DBMS connection into the hibernate properties files:
```
src > main > resources > META-INF > persistence.xml
src > test > resources > META-INF > persistence.xml
```
3. Update the ```src > main > resources > application.properties``` file.
    1. Set ```outputFolder``` to specify where the quad files will be written to.
    2. Set ```htmlExtractorServiceURI``` to specify the location of the fallback HTML extractor. You don't have to do this, and you can ignore the generated errors if you wish.
    3. [OPTIONAL] Set ```waitTime``` to determine how long each thread waits before starting a new crawl. Currently 1 second.
    4. [OPTIONAL] Set ```totalNumberOfPagesToCrawlInASession``` to determine how many URLs are crawled in a single run. Currently set to 100,000.
    5. [OPTIONAL] A number of URLs are taken into memory at once and crawled. Then written to the DBMS before another set are obtained (provided the total number of URLs is less than ```totalNumberOfPagesToCrawlInASession```). To determine how many are scraped in each loop set ```numberOfPagesToCrawlInALoop```. Currently 1000 URLs are scraped each loop until 100,000 have been scraped at which point the program closes.
4. Run maven compile (or package)
5. You should then be able to run the code, e.g.,
```
 mvn exec:java -Dexec.mainClass="hwu.elixir.scrape.ThreadedScrapeDriver"
```


## Funding

A project by [SWeL](http://www.macs.hw.ac.uk/SWeL/) funded through [Elixir-Excelerate](https://elixir-europe.org/about-us/how-funded/eu-projects/excelerate). 

<br />
<br />

***

<a href="https://www.hw.ac.uk"><img src="https://www.hw.ac.uk/dist/assets/images/logo@2x.webp" alt="hwu logo" height="40" /> </a> <span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span> <a href="https://elixir-europe.org/about-us/how-funded/eu-projects/excelerate"><img src="https://www.elixir-europe.org/sites/default/files/images/excelerate_whitebackground.png" alt="elixir-excelerate logo" height="40"/></a>

