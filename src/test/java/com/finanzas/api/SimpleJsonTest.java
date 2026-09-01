package com.finanzas.api;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SimpleJsonTest {
    @Test
    void parsesDecimalNumbersAsBigDecimal() throws Exception {
        Map<String, Object> json = SimpleJson.asObject(SimpleJson.parse("{\"amount\":1234567890.12}"));

        assertInstanceOf(BigDecimal.class, json.get("amount"));
        assertEquals(new BigDecimal("1234567890.12"), SimpleJson.decimal(json, "amount"));
    }

    @Test
    void stringifiesNestedArraysAndObjects() {
        Map<String, Object> participant = new LinkedHashMap<String, Object>();
        participant.put("userId", "u1");
        participant.put("amount", new BigDecimal("100.00"));
        List<Map<String, Object>> participants = new ArrayList<Map<String, Object>>();
        participants.add(participant);
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("participants", participants);

        assertEquals("{\"participants\":[{\"userId\":\"u1\",\"amount\":100.00}]}", SimpleJson.stringify(root));
    }
}
