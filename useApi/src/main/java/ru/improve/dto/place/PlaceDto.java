package ru.improve.dto.place;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlaceDto {

    private List<Feature> features;
}
