package ru.improve.dto.weather;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WeatherDto {

    private List<Weather> weather;

    private WeatherMain main;

    private int visibility;

    private Wind wind;

    private Clouds clouds;

    private long dt;
}
