package ru.improve.dto.area;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Hit {

    private long osm_id;

    private String name;

    private String country;

    private String place;

    private String state;

    private String street;

    private String housenumber;

    private Point point;
}
