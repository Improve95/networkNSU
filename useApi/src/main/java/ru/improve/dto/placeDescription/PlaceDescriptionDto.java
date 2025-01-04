package ru.improve.dto.placeDescription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlaceDescriptionDto {

    private String name;

    private Address address;

    private String kinds;
}
