package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

/**
 * Extending functionality found in crawlInternal for the sequential
 * implementation into a RecursiveAction for parallel implementation.
 */
public final class CrawlAction extends RecursiveAction {
    private final Clock clock;
    private final String url;
    private final Instant deadline;
    private final int maxDepth;
    private final PageParserFactory parserFactory;
    private final Map<String, Integer> counts;
    private final Set<String> visitedUrls;
    private final List<Pattern> ignoredUrls;

    private CrawlAction(String url, Instant deadline, int maxDepth, PageParserFactory parserFactory, Clock clock, Map<String, Integer> counts, Set<String> visitedUrls, List<Pattern> ignoredUrls) {
        this.url = url;
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.parserFactory = parserFactory;
        this.clock = clock;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.ignoredUrls = ignoredUrls;
    }

    @Override
    protected void compute() {
        //mostly the same logic as crawlInternal
        if (maxDepth == 0 || clock.instant().isAfter(deadline))  {
            return;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }
        if (visitedUrls.contains(url)) {
            return;
        }
        visitedUrls.add(url);
        PageParser.Result result = parserFactory.get(url).parse();

        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            if (counts.containsKey(e.getKey())) {
                counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
            } else {
                counts.put(e.getKey(), e.getValue());
            }
        }

        //however instead of a recursive call to self we make a list of subtasks then invokeAll
        List<CrawlAction> subtasks = new ArrayList<>();

        CrawlAction.Builder crawlAction = new Builder().setDeadline(deadline)
                .setMaxDepth(maxDepth-1)
                .setParserFactory(parserFactory)
                .setClock(clock)
                .setCounts(counts)
                .setVisitedUrls(visitedUrls)
                .setIgnoredUrls(ignoredUrls);

        for (String link : result.getLinks()) {
            subtasks.add(crawlAction.setUrl(link).build());
        }
        invokeAll(subtasks);
    }

    public static final class Builder {
        private Clock clock;
        private String url;
        private Instant deadline;
        private int maxDepth;
        private PageParserFactory parserFactory;
        private Map<String, Integer> counts;
        private Set<String> visitedUrls;
        private List<Pattern> ignoredUrls;

        /**
         * Sets the url.
         */
        public CrawlAction.Builder setUrl(String url) {
            this.url = Objects.requireNonNull(url);
            return this;
        }

        /**
         * Sets the deadline.
         */
        public CrawlAction.Builder setDeadline(Instant deadline) {
            this.deadline = deadline;
            return this;
        }

        /**
         * Sets the clock.
         */
        public CrawlAction.Builder setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        /**
         * Sets the parserFactory.
         */
        public CrawlAction.Builder setParserFactory(PageParserFactory parserFactory) {
            this.parserFactory = parserFactory;
            return this;
        }

        /**
         * Sets the clock.
         */
        public CrawlAction.Builder setClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Sets the counts.
         */
        public CrawlAction.Builder setCounts(Map<String, Integer> counts) {
            this.counts = counts;
            return this;
        }

        /**
         * Sets the visitedUrls.
         */
        public CrawlAction.Builder setVisitedUrls(Set<String> visitedUrls) {
            this.visitedUrls = visitedUrls;
            return this;
        }

        /**
         * Sets the ignoredUrls.
         */
        public CrawlAction.Builder setIgnoredUrls(List<Pattern> ignoredUrls) {
            this.ignoredUrls = ignoredUrls;
            return this;
        }


        /**
         * Constructs a {@link CrawlAction} from this builder.
         */
        public CrawlAction build() {
            return new CrawlAction(url, deadline, maxDepth, parserFactory, clock, counts, visitedUrls, ignoredUrls);
        }
    }

}