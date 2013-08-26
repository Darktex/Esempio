Usage

Crawl a list of cities: java -Xmx3g -jar YelpScraper.jar --mineAllCities cityA cityB ... cityN
Crawl a list of cities starting from page n (on each one): java -Xmx3g -jar YelpScraper.jar --startFrom n cityA cityB ... cityN
Crawl a list of cities starting from page n, ending in page m (on each one): java -Xmx3g -jar YelpScraper.jar --mineBetween n m cityA cityB ... city N

NOTE: My experience is that you receive a ban after ~220k GET requests. The script lets you input a list of cities, but you may want to scrape one at a time if it's too big
