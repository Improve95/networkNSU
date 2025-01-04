package ru.improve.dto.place;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlaceProperties {

    private String xid;

    private String name;

    private double dist;

    private int rate;

    private String kinds;
}
