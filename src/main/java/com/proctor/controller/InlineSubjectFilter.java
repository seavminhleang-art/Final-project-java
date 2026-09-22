package com.proctor.controller;

import com.proctor.util.KeyUtil;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.util.ArrayList;
import java.util.List;

public final class InlineSubjectFilter<T> {

    public static class Item<T> {
        private final T value;
        private final String code;
        private final String label;

        public Item(T value, String code, String label) {
            this.value = value;
            this.code = (code != null) ? code : "";
            this.label = (label != null) ? label : "";
        }

        public T getValue() { return value; }
        public String getCode() { return code; }
        public String getLabel() { return label; }
    }

    private final List<Item<T>> allItems = new ArrayList<>();
    private final StringBuilder query = new StringBuilder();
    private final List<Item<T>> matches = new ArrayList<>();
    private int matchIndex = 0;
    private Item<T> selectedItem = null;
    private Item<T> originalItem = null;
    private boolean active = false;

    public InlineSubjectFilter(List<Item<T>> items) {
        setItems(items);
    }

    public final void setItems(List<Item<T>> items) {
        this.allItems.clear();
        if (items != null) {
            this.allItems.addAll(items);
        }
        recomputeMatches();
        if (!allItems.isEmpty() && selectedItem == null) {
            selectedItem = allItems.get(0);
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            query.setLength(0);
            recomputeMatches();
        }
    }

    public void startSearch(int currentOriginalIndex) {
        this.active = true;
        this.query.setLength(0);
        if (currentOriginalIndex >= 0 && currentOriginalIndex < allItems.size()) {
            this.originalItem = allItems.get(currentOriginalIndex);
            this.selectedItem = this.originalItem;
        } else if (!allItems.isEmpty()) {
            this.originalItem = allItems.get(0);
            this.selectedItem = this.originalItem;
        }
        recomputeMatches();
    }

    public void cancelSearch() {
        this.active = false;
        this.query.setLength(0);
        if (originalItem != null) {
            this.selectedItem = originalItem;
        }
        recomputeMatches();
    }

    public void confirmSearch() {
        this.active = false;
        this.query.setLength(0);
        recomputeMatches();
    }

    public String getQuery() {
        return query.toString();
    }

    public Item<T> getSelectedItem() {
        return selectedItem;
    }

    public T getSelectedValue() {
        return selectedItem != null ? selectedItem.getValue() : null;
    }

    public int getSelectedOriginalIndex() {
        if (selectedItem == null) return -1;
        return allItems.indexOf(selectedItem);
    }

    public void setSelectedOriginalIndex(int index) {
        if (index >= 0 && index < allItems.size()) {
            this.selectedItem = allItems.get(index);
            this.originalItem = this.selectedItem;
            recomputeMatches();
        }
    }

    public boolean handleKey(KeyPressMessage k) {
        if (KeyUtil.isEnter(k)) {
            confirmSearch();
            return true;
        }
        if (KeyUtil.isEsc(k)) {
            cancelSearch();
            return true;
        }
        if (KeyUtil.isTab(k) || KeyUtil.isRight(k)) {
            cycleNext();
            return true;
        }
        if (KeyUtil.isLeft(k)) {
            cyclePrev();
            return true;
        }
        if (KeyUtil.isBackspace(k)) {
            if (query.length() > 0) {
                query.deleteCharAt(query.length() - 1);
                recomputeMatches();
            }
            return true;
        }
        if (k.type() == KeyType.KeyRunes && k.runes() != null) {
            boolean changed = false;
            for (char c : k.runes()) {
                if (!Character.isISOControl(c)) {
                    query.append(c);
                    changed = true;
                }
            }
            if (changed) {
                active = true;
                recomputeMatches();
            }
            return changed;
        }
        if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
            query.append(k.key());
            active = true;
            recomputeMatches();
            return true;
        }
        return false;
    }

    public void cycleNext() {
        if (matches.isEmpty()) return;
        matchIndex = (matchIndex + 1) % matches.size();
        selectedItem = matches.get(matchIndex);
    }

    public void cyclePrev() {
        if (matches.isEmpty()) return;
        matchIndex = (matchIndex - 1 + matches.size()) % matches.size();
        selectedItem = matches.get(matchIndex);
    }

    private void recomputeMatches() {
        matches.clear();
        String q = query.toString().trim().toLowerCase();
        if (q.isEmpty()) {
            matches.addAll(allItems);
        } else {
            for (Item<T> item : allItems) {
                boolean matchCode = item.getCode().toLowerCase().contains(q);
                boolean matchLabel = item.getLabel().toLowerCase().contains(q);
                if (matchCode || matchLabel) {
                    matches.add(item);
                }
            }
        }

        if (matches.isEmpty()) {
            matchIndex = -1;
        } else {
            int idx = -1;
            if (selectedItem != null) {
                for (int i = 0; i < matches.size(); i++) {
                    if (matches.get(i) == selectedItem) {
                        idx = i;
                        break;
                    }
                }
            }
            matchIndex = (idx >= 0) ? idx : 0;
            selectedItem = matches.get(matchIndex);
        }
    }

    public String getHeaderDisplay() {
        if (!active) {
            return selectedItem != null ? selectedItem.getCode() : "ALL";
        }
        if (matches.isEmpty()) {
            return "[ " + query + "_ ] → (No match)";
        }
        String code = selectedItem != null ? selectedItem.getCode() : "ALL";
        return "[ " + query + "_ ] → " + code + " (" + (matchIndex + 1) + "/" + matches.size() + ")";
    }

    public String getFormDisplay(String fallbackDefault) {
        if (!active || query.length() == 0) {
            if (selectedItem != null && !selectedItem.getLabel().isBlank()) {
                return selectedItem.getLabel();
            }
            return (fallbackDefault != null) ? fallbackDefault : "-- Select Subject --";
        }
        if (matches.isEmpty()) {
            return "(No matches for \"" + query + "\")";
        }
        String label = (selectedItem != null) ? selectedItem.getLabel() : "";
        return label + " (Search: \"" + query + "\" • " + (matchIndex + 1) + "/" + matches.size() + ", Tab to cycle)";
    }
}
