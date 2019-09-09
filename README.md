# Scraper

## Description


### Design decisions

* Using Apache Any23 to parse structured data from HTML.
* Using [chromium headless driver](https://chromedriver.chromium.org/) with [Selenium](https://www.seleniumhq.org/) to load pages.
* Quads are used as basic provenance is captured at the page/context/graph level.
* Quads are not automatically loaded into the triplestore as that is very slow, and scraping is slow enough. Saving to file then bulk importing is much quicker. This also means the enduser can choose their own triplestore.


## Build instructions

Requirements:
* maven v3.6.0
* mysql v8.0.13
* other requirements are provided through pom file

### Instructions for running




## Funding

A project by [SWeL](http://www.macs.hw.ac.uk/SWeL/) funded through [Elixir-Excelerate](https://elixir-europe.org/about-us/how-funded/eu-projects/excelerate). 

<br />
<br />

***

<a href="https://www.hw.ac.uk"><img src="https://www.hw.ac.uk/dist/assets/images/logo@2x.webp" alt="hwu logo" height="40" /> </a> <span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span> <a href="https://elixir-europe.org/about-us/how-funded/eu-projects/excelerate"><img src="https://www.elixir-europe.org/sites/default/files/images/excelerate_whitebackground.png" alt="elixir-excelerate logo" height="40"/></a>

