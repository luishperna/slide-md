package com.slidemd.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DirectiveAutocompleteTest {

    @Test
    void testFilterSuggestionsByPrefix() {
        String query = "backg";
        String cleanQ = query.toLowerCase().trim();

        List<DirectiveAutocompletePopup.SuggestionItem> filtered = DirectiveAutocompletePopup.ALL_COMMANDS_AND_DIRECTIVES.stream()
                .filter(it -> it.displayText().toLowerCase().contains(cleanQ)
                           || it.insertText().toLowerCase().contains(cleanQ)
                           || it.description().toLowerCase().contains(cleanQ))
                .sorted((a, b) -> {
                    String aClean = a.displayText().toLowerCase().replace(":", "").trim();
                    String bClean = b.displayText().toLowerCase().replace(":", "").trim();
                    boolean aStarts = aClean.startsWith(cleanQ);
                    boolean bStarts = bClean.startsWith(cleanQ);
                    if (aStarts && !bStarts) return -1;
                    if (!aStarts && bStarts) return 1;
                    return 0;
                })
                .toList();

        assertFalse(filtered.isEmpty());
        assertEquals("background: ", filtered.get(0).insertText());
    }

    @Test
    void testFilterSuggestionsAlignment() {
        String query = "ali";
        String cleanQ = query.toLowerCase().trim();

        List<DirectiveAutocompletePopup.SuggestionItem> filtered = DirectiveAutocompletePopup.ALL_COMMANDS_AND_DIRECTIVES.stream()
                .filter(it -> it.displayText().toLowerCase().contains(cleanQ)
                           || it.insertText().toLowerCase().contains(cleanQ)
                           || it.description().toLowerCase().contains(cleanQ))
                .sorted((a, b) -> {
                    String aClean = a.displayText().toLowerCase().replace(":", "").trim();
                    String bClean = b.displayText().toLowerCase().replace(":", "").trim();
                    boolean aStarts = aClean.startsWith(cleanQ);
                    boolean bStarts = bClean.startsWith(cleanQ);
                    if (aStarts && !bStarts) return -1;
                    if (!aStarts && bStarts) return 1;
                    return 0;
                })
                .toList();

        assertFalse(filtered.isEmpty());
        assertEquals("alignment: ", filtered.get(0).insertText());
    }

    @Test
    void testFilterSuggestionsSound() {
        String query = "sou";
        String cleanQ = query.toLowerCase().trim();

        List<DirectiveAutocompletePopup.SuggestionItem> filtered = DirectiveAutocompletePopup.ALL_COMMANDS_AND_DIRECTIVES.stream()
                .filter(it -> it.displayText().toLowerCase().contains(cleanQ)
                           || it.insertText().toLowerCase().contains(cleanQ)
                           || it.description().toLowerCase().contains(cleanQ))
                .toList();

        assertFalse(filtered.isEmpty());
        assertTrue(filtered.stream().anyMatch(it -> it.insertText().equals("sound: ")));
    }

    @Test
    void testFilterValuesAlignment() {
        List<DirectiveAutocompletePopup.SuggestionItem> values = DirectiveAutocompletePopup.ALIGN_VALUES;
        assertEquals(5, values.size());
        assertTrue(values.stream().anyMatch(it -> it.insertText().equals("center")));
        assertTrue(values.stream().anyMatch(it -> it.insertText().equals("left")));
        assertTrue(values.stream().anyMatch(it -> it.insertText().equals("right")));
        assertTrue(values.stream().anyMatch(it -> it.insertText().equals("top")));
        assertTrue(values.stream().anyMatch(it -> it.insertText().equals("down")));
    }
}
