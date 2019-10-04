# Scraper

## Description

There are 3 sub-modules:
* *core* provides core scraping functionality
* *service* extends this to be a process that can be run via the command line. URLs to be scraped are read from a database.
* *web* turns core into a very basic webapp that scrapes a single URL fed into the app

### Design decisions

* Using [Apache Any23](https://any23.apache.org/) to parse structured data from HTML.
    * Apache Any23 is believed to have the best rdfa parser for JAVA; in the sense that it does not require the HTML to be perfect.
    * If not using rdfa, simply pulling the HTML and extracting the div blocks using [JSoup](https://jsoup.org/) is faster.
* Using [chrome headless driver](https://chromedriver.chromium.org/) with [Selenium](https://www.seleniumhq.org/) to load pages. As pages are increasingly dynamic the JS on each page needs to be processed.
* Quads are used as basic provenance is captured at the page/context/graph level.
* Quads are not automatically loaded into the triplestore as that is very slow, and scraping is slow enough. Saving to file then bulk importing is much quicker. This also means the enduser can choose their own triplestore.


## Build instructions

Requirements:
* java 1.8
* maven v3.6.0
* mysql v8.0.13
* google chrome browser and [driver](https://chromedriver.chromium.org/); you must select the same version for both, e.g., v77.
* other requirements are provided through pom file

### Instructions for running

#### core

Cannot be run.

#### service

1. You may want to set the [JVM parameters](https://stackoverflow.com/questions/14763079/what-are-the-xms-and-xmx-parameters-when-starting-jvm) to increase the size of RAM available to JAVA.
2. Add your database connection to hibernate; we are using `service > src > main > resources > META-INF > persistence.xml`.
3. Update `service > src > main > resources > applications.properties`. You need to specify:
    * how long you want to wait being fetching pages (default: 1 second).
    * output loction: currently all RDF is saved to a folder. 
    * how many pages you want to crawl in a single loop (default: 8).
    * how many pages you want to crawl in a single session; there are multiple loops in a session (default: 32).
    * location of the chrome driver.
4. Package with maven: `mvn clean package` from the top level, i.e., *Scraper* folder not the *service* folder.
5. Inside the `service > target` directory you will find `service.jar`. Run it however you wish via maven or the command line, e.g., `java -jar service.jar`.

#### service

Still in development so use is not recommended.

## Funding

A project by [SWeL](http://www.macs.hw.ac.uk/SWeL/) funded through [Elixir-Excelerate](https://elixir-europe.org/about-us/how-funded/eu-projects/excelerate). 

<br />
<br />

***

<a href="https://www.hw.ac.uk"><img src="https://www.hw.ac.uk/dist/assets/images/logo@2x.webp" alt="hwu logo" height="40" /> </a> <span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span> <a href="https://elixir-europe.org/about-us/how-funded/eu-projects/excelerate"><img src="https://www.elixir-europe.org/sites/default/files/images/excelerate_whitebackground.png" alt="elixir-excelerate logo" height="40"/></a>

