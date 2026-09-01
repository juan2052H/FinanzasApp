package com.finanzas.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FinancialHealthSnapshot {
    private final int score;
    private final String label;
    private final List<String> strengths;
    private final List<String> opportunities;

    public FinancialHealthSnapshot(int score, String label, List<String> strengths, List<String> opportunities) {
        this.score = Math.max(0, Math.min(100, score));
        this.label = label == null ? "" : label;
        this.strengths = Collections.unmodifiableList(new ArrayList<String>(strengths));
        this.opportunities = Collections.unmodifiableList(new ArrayList<String>(opportunities));
    }

    public int getScore() { return score; }
    public String getLabel() { return label; }
    public List<String> getStrengths() { return strengths; }
    public List<String> getOpportunities() { return opportunities; }
}
