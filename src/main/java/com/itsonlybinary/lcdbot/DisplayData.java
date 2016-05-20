/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itsonlybinary.lcdbot;

/**
 *
 * @author ItsOnlyBinary
 */
public class DisplayData {

    private int row = 1;
    private int col = 0;
    private String text;
    private boolean scroll = true;
    private int stage = 0;
    private boolean done = false;

    DisplayData(int row, int col, String text, boolean scroll) {
        this.row = row;
        this.col = col;
        this.scroll = scroll;
        if (scroll) {
            this.text = "                " + text + "                ";
        } else {
            this.text = text;
        }
        while (col + this.text.length() < 16) {
            this.text += " ";
        }
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

    public int getStage() {
        return stage;
    }

    public void incStage() {
        stage++;
    }

    boolean isScroll() {
        return scroll;
    }

    boolean isDone() {
        return done;
    }
}