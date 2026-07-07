package com.mynyaman.mynyamanapp;

import android.content.Context;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CyberbullyingDetector {

    private static final Set<String> DEFAULT_MILD_TERMS = new LinkedHashSet<>(Arrays.asList(
            "dumb",
            "idiot",
            "stupid",
            "shit",
            "loser",
            "moron",
            "ugly",
            "pathetic",
            "freak",
            "trash",
            "annoying",
            "weirdo",
            "lame",
            "shut up",
            "hate you"
    ));

    private static final Set<String> DEFAULT_SEVERE_TERMS = new LinkedHashSet<>(Arrays.asList(
            "kill yourself",
            "go die",
            "die already",
            "i will hurt you",
            "i'm going to hurt you",
            "nobody wants you",
            "everyone hates you",
            "you're worthless",
            "you are worthless",
            "i will find you",
            "i'll find you",
            "rape",
            "slut",
            "whore"
    ));

    private static final Set<String> DEFAULT_EXPLICIT_TERMS = new LinkedHashSet<>(Arrays.asList(
            "nude",
            "nudes",
            "send nudes",
            "porn",
            "horny",
            "dick",
            "pussy",
            "cum",
            "sex tape",
            "blowjob",
            "fuck me"
    ));

    private static final List<Pattern> HARASSMENT_PATTERNS = Arrays.asList(
            Pattern.compile("\\b(you are|you're|ur)\\s+(so\\s+)?(stupid|dumb|ugly|worthless|pathetic)\\b"),
            Pattern.compile("\\b(no one likes you|everyone hates you|go die|kill yourself)\\b"),
            Pattern.compile("\\b(i hate you|you disgust me|you're a loser)\\b")
    );

    private CyberbullyingDetector() {
    }

    public static RuleScanResult scan(Context context, String normalizedText) {
        if (TextUtils.isEmpty(normalizedText)) {
            return new RuleScanResult(ThreatResult.Level.GREEN, 0, 0, 0, new ArrayList<>());
        }

        Set<String> mildTerms = ThreatDictionaryStore.getMergedMildTerms(context, DEFAULT_MILD_TERMS);
        Set<String> severeTerms = ThreatDictionaryStore.getMergedSevereTerms(context, DEFAULT_SEVERE_TERMS);
        Set<String> explicitTerms = ThreatDictionaryStore.getMergedExplicitTerms(context, DEFAULT_EXPLICIT_TERMS);

        List<String> matchedTerms = new ArrayList<>();
        int mildMatches = countMatches(normalizedText, mildTerms, matchedTerms);
        int severeMatches = countMatches(normalizedText, severeTerms, matchedTerms);
        int explicitMatches = countMatches(normalizedText, explicitTerms, matchedTerms);

        for (Pattern pattern : HARASSMENT_PATTERNS) {
            Matcher matcher = pattern.matcher(normalizedText);
            if (matcher.find()) {
                severeMatches++;
                matchedTerms.add(matcher.group());
            }
        }

        ThreatResult.Level level = ThreatResult.Level.GREEN;
        if (severeMatches > 0 || explicitMatches > 0) {
            level = ThreatResult.Level.RED;
        } else if (mildMatches > 0) {
            level = ThreatResult.Level.YELLOW;
        }

        if (mildMatches >= 3 && level == ThreatResult.Level.YELLOW) {
            level = ThreatResult.Level.RED;
        }

        return new RuleScanResult(level, mildMatches, severeMatches, explicitMatches, matchedTerms);
    }

    private static int countMatches(String normalizedText, Set<String> terms, List<String> matchedTerms) {
        int count = 0;
        for (String term : terms) {
            if (TextUtils.isEmpty(term)) {
                continue;
            }
            String safeTerm = term.toLowerCase(Locale.US);
            if (normalizedText.contains(safeTerm)) {
                count++;
                matchedTerms.add(term);
            }
        }
        return count;
    }

    public static final class RuleScanResult {
        public final ThreatResult.Level level;
        public final int mildMatches;
        public final int severeMatches;
        public final int explicitMatches;
        public final List<String> matchedTerms;

        RuleScanResult(
                ThreatResult.Level level,
                int mildMatches,
                int severeMatches,
                int explicitMatches,
                List<String> matchedTerms
        ) {
            this.level = level;
            this.mildMatches = mildMatches;
            this.severeMatches = severeMatches;
            this.explicitMatches = explicitMatches;
            this.matchedTerms = matchedTerms;
        }
    }
}
