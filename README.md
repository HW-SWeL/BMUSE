# Scraper

## Description


### Design decisions

* Virtuoso was selected as the triplestore because it can handle very large numbers of triples with the free version.

* Quads are not automatically loaded into the triplestore as that is very slow, and scraping is slow enough. Saving to file then bulk importing is much quicker.


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

