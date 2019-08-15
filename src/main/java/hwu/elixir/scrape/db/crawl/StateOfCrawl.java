package hwu.elixir.scrape.db.crawl;

public enum StateOfCrawl {
	DOES_NOT_EXIST, HUMAN_INSPECTION, UNTRIED, FAILED, FAILED_TWICE, NODE_FAILED, GIVEN_UP, SUCCESS;
}
