package ru.improve.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class FieldData {

    private int currentFieldWidth = 0;

    private int currentFieldHeight = 0;

    public void setFieldSize(int width, int height) {
        this.currentFieldWidth = width;
        this.currentFieldHeight = height;
    }

    public int calculateCoordX(int x) {
        if (x == -1) {
            return currentFieldWidth - 1;
        } else if (x == currentFieldWidth) {
            return 0;
        } else if (-1 < x && x < currentFieldWidth) {
            return x;
        } else {
            throw new RuntimeException("x coord out of bounds field: " + x + "; field width: " + currentFieldWidth);
        }
    }

    public int calculateCoordY(int y) {
        if (y == -1) {
            return currentFieldHeight - 1;
        } else if (y == currentFieldHeight) {
            return 0;
        } else if (-1 < y && y < currentFieldHeight) {
            return y;
        } else {
            throw new RuntimeException("y coord out of bounds field: " + y + "; field height: " + currentFieldHeight);
        }
    }
}
