package com.itsonlybinary.lcdbot;

/**
 * @author ItsOnlyBinary
 */
public class DisplayData {

    private int row = 1;
    private int col = 0;
    private String text;
    private boolean scroll = true;
    private int stage = 0;
    private boolean done = false;

    public DisplayData(int row, int col, String text, boolean scroll) {
        this.row = row;
        this.col = col;
        this.scroll = scroll;
        if (scroll) {
            this.text = "                " + cleanText(text) + "                ";
        } else {
            this.text = cleanText(text);
        }
        while (col + this.text.length() < 16) {
            this.text += " ";
        }
    }

    private String cleanText(String text) {
        StringBuilder sb = new StringBuilder(text);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 0x20 || c > 0x7d || c == 0x5c) {
                sb.setCharAt(i, (char) 0x7e);
            }
        }
        return sb.toString();
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public String getText() {
        String outText = (scroll) ? text.substring(stage, stage + 16) : text;

        if (!scroll || stage >= text.length() - 16) {
            done = true;
        } else {
            stage++;
        }
        return outText;
    }

    boolean isDone() {
        return done;
    }
}
