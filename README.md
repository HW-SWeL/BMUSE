# Scraper

A scraper designed to scrape [Bioschema's markup](https://www.bioschemas.org), in either JSON-LD or RDFa, from a set of known web pages.
Implementation decisions are discussed [here](https://github.com/HW-SWeL/Scraper/wiki/Decisions).

## Description

There are 3 sub-modules:
* *core* provides core scraping functionality.
* *service* extends *core* to be a process that can be run via the command line. URLs to be scraped are read from a database.
* *web* turns *core* into a very basic webapp that scrapes a single URL fed into the app.

### Design decisions

* Using [Apache Any23](https://any23.apache.org/) to parse structured data from HTML.
    * Apache Any23 is believed to have the best rdfa parser for JAVA; in the sense that it does not require the HTML to be perfect.
    * If not using rdfa, simply pulling the HTML and extracting the div blocks using [JSoup](https://jsoup.org/) is faster.
* Using [chrome headless driver](https://chromedriver.chromium.org/) with [Selenium](https://www.seleniumhq.org/) to load pages. As pages are increasingly dynamic the JS on each page needs to be processed.
* Quads are used as basic provenance is captured at the page/context/graph level.
* Quads are not automatically loaded into a triplestore as that is very slow, and scraping is slow enough. Saving to file then bulk importing is much quicker. This also means the end user can choose their own triplestore.


## Build instructions

Requirements for *core*:
* java 1.8+
* maven v3.6.0
* google chrome browser and [driver](https://chromedriver.chromium.org/); you must select the same version for both, e.g., v77.
* other requirements are provided through pom file.

Additional requirements for *service*:
* mysql v8.0.13
* appropriate [chromium driver](https://chromedriver.chromium.org/) for your version of chrome 
* other requirements are provided through pom file

*Web* has no additional requirements, but you require some way of running war files.


### Instructions for running

First clone the repo to your machine. *Core* is relied on by both *service* and *web*. However, *core* can be used in a standalone manner.

#### core

Provides the core functionality as an abstract class. Additionally, two example classes exist that can be used to scrape either a single given URL or a series of URLs from a given file. For most purposes this file scraper is likely to be sufficient and there is no need to explore further. If you follow the instructions below you will run the file scraper.

To use this:

1/ Default configuration is read from `core > src > main > resources > configuration.properties`. To override some properties, create file `localconfig.properties` in the directory where you will run the application, and give the new values of the properties as needed:
* `outputFolder`: currently all RDF is saved as NQuads to a folder.
* `locationOfSitesFile`: location of the list of URLs you wish to scrape located. There is an example in `core > src > main > resources > urls2scrape.txt`. Please note that you can set dynamic or static parsing on a per URL basis by adding a comma and static or dynamic after that to change the way that URL is scraped on the `urls2scrape.txt` file.
* `chromiumDriverLocation`: full path to the Chrome driver file. On Windows this will be called `chromedriver.exe`
* `maxLimitScrape`: maximum number of URLs to scrape (defaults to 5)
* `schemaContext`: path to the Schema.org context file
* `dynamic`: boolean setting (true or false) that will set the scraper to dynamic or static markup parsing

A typical localconfig.properties file for **Linux** will look like this:

```
chromiumDriverLocation = /home/username/chrome/chromedriver
locationOfSitesFile = /home/username/bmuse/urls2scrape.txt
outputFolder = /home/username/bmuse/
maxLimitScrape = 100
```

A typical localconfig.properties file for **Windows** will look like this:

```
chromiumDriverLocation = C\:/Users/username/chrome/chromedriver.exe
locationOfSitesFile = C\:/Users/username/bmuse/urls2scrape.txt
outputFolder = C\:/Users/username/bmuse/
maxLimitScrape = 100
```
    
2/ Create/edit your list of urls file.

3/ Package with maven: `mvn clean package` 
   * If you only want to compile/run *core* you can do this from inside the *core* directory.
   * If you also want to use *service* or *web* then run maven from the top level *Scraper* folder.
- Inside the `core > target` directory you will find two jars. The fat jar is called `core-x.x.x-SNAPSHOT.jar` and the skinny jar is `original-core-x.x.x-SNAPSHOT.jar`. 

4/ Run the fat jar via maven or the command line: `java -jar core-x.x.x-SNAPSHOT.jar`

This will run the `hwu.elixir.scrape.scraper.examples.FileScrapper` main class.

**UTF-8 character encoding**. On Windows systems, you need to force the UTF-8 charset with `-Dfile.encoding=UTF-8`.

```shell
java -Dfile.encoding=UTF-8 -jar core-x.x.x-SNAPSHOT.jar
```

**Log configuration**. You may also override the default log configuration by copying `src > main > resources > logback.xml` to your own file and run the application as follows:

```bash
java -Dlogback.configurationFile=./logback.xml -Dfile.encoding=UTF-8 -jar core-x.x.x-SNAPSHOT.jar
```

Note: file `localconfig.Properties` will be saved back with additional property `contextCounter`: this is an auto-incrementing count of the number of sites scraped. You can reset this count to 0 or simply delete the property from your `localconfig.properties` file.



#### service

Assumes a database of URLs that need to be scraped. Will collect a list of URLs from the database, scrape them and write the output to a specified folder. The output will be in NQuads format.

To use this:
1. You may want to set the [JVM parameters](https://stackoverflow.com/questions/14763079/what-are-the-xms-and-xmx-parameters-when-starting-jvm) to increase the size of RAM available to JAVA.
2. Add your database connection to hibernate; we are using `service > src > main > resources > META-INF > persistence.xml`.
3. If your database is empty, running the program (by following the steps below) will create an empty table before stopping as there are no URLs to scrape. You can then populate this table and re-run the program to perform the scrape. Alternatively, you can create the table and populate the database manually. An example script for this can be found in `service > src > main > resources > setUpDatabaseScript.sql`. If you run this before running the program, it will start scraping immediately.
4. Update `service > src > main > resources > applications.properties`. You need to specify:
    * how long you want to wait being fetching pages, measured in tenths of a second. (default: 5 = 0.5 second).
    * output location: currently all RDF is saved as NQuads to a folder. 
    * how many pages you want to crawl in a single loop (default: 8).
    * how many pages you want to crawl in a single session; there are multiple loops in a session (default: 32). The default settings are enough for you to run the scraper to check everything is working. However, these should be increased for a real world scrape.
    * location of the chrome driver.
5. Package with maven: `mvn clean package` from the top level, i.e., *Scraper* folder not the *service* folder.
6. Inside the `service > target` directory you will find `service.jar`. Run it however you wish via maven or the command line, e.g., `java -jar service.jar`.

#### web

Still in development so use is not recommended.
Goal: to provide a small web app that receives a URL as a request and returns the (bio)schema markup from that URL in a JSON format.


## Funding

A project by [SWeL](http://www.macs.hw.ac.uk/SWeL/) funded through [Elixir-Excelerate](https://elixir-europe.org/about-us/how-funded/eu-projects/excelerate). 

<br />
<br />

***

<a href="https://www.hw.ac.uk"><img src="https://www.hw.ac.uk/dist/assets/images/logo@2x.webp" alt="hwu logo" height="40" /> </a> <span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span> <a href="https://elixir-europe.org/about-us/how-funded/eu-projects/excelerate"><img src="https://www.elixir-europe.org/sites/default/files/images/excelerate_whitebackground.png" alt="elixir-excelerate logo" height="40"/></a>

