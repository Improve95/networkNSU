package ru.improve.dto.placeDescription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Address {

    private String city;

    private String road;

    private String state;

    private String county;

    private String suburb;

    private String country;

    private String postcode;

    private String country_code;
}
