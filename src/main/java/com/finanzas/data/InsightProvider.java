package com.finanzas.data;

import java.util.List;

public interface InsightProvider {
    List<String> generateInsights(DataManager data);
}
