package ru.improve.place;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import okhttp3.Response;
import ru.improve.config.Property;
import ru.improve.dto.area.FindAreaDto;
import ru.improve.dto.area.Hit;
import ru.improve.dto.area.Point;
import ru.improve.dto.place.Feature;
import ru.improve.dto.place.PlaceDto;
import ru.improve.dto.placeDescription.PlaceDescriptionDto;
import ru.improve.dto.weather.WeatherDto;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class FindPlace {

    private Properties property = Property.getInstance();

    private BufferedReader bf;

    private FindAreaDto findAreaDto;

    public FindPlace(BufferedReader bf) {
        this.bf = bf;
    }

    public CompletableFuture<Void> find() throws IOException {
        CompletableFuture<Point> findAreaFuture = CompletableFuture.supplyAsync(
                () -> {
                    try {
                        String locationName = bf.readLine();
                        FindAreaDto findAreaDto = findArea(locationName);
                        int counter = 0;
                        for (Hit hit : findAreaDto.getHits()) {
                            System.out.println((counter++) + " " + hit.toString());
                        }
                        int locationChoice = Integer.parseInt(bf.readLine());
                        return findAreaDto.getHits().get(locationChoice).getPoint();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        CompletableFuture<WeatherDto> findWeatherFuture = findAreaFuture.thenApplyAsync(
                point -> {
                    try {
                        WeatherDto weatherDto = getWeatherByPoint(point);
                        return weatherDto;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        CompletableFuture<PlaceDto> findPlacesFuture = findAreaFuture.thenApplyAsync(
                point -> {
                    try {
                        return findPlaces(point, 10000);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        CompletableFuture<List<PlaceDescriptionDto>> findPlaceDescriptionFuture = findPlacesFuture.thenApplyAsync(
                placeDto -> {
                    try {
                        List<PlaceDescriptionDto> placeDescriptionList = new ArrayList<>();
                        for (Feature feature : placeDto.getFeatures()) {
                            String xid = feature.getProperties().getXid();
                            placeDescriptionList.add(findPlaceDescription(xid));
                        }
                        return placeDescriptionList;
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
        );

        CompletableFuture<Void> combineFuture = findWeatherFuture.thenCombine(findPlaceDescriptionFuture,
                (weatherDto, PlaceDescriptionList) -> {
                    System.out.println("\n" + "==================" + "\n");
                    System.out.println(weatherDto);
                    System.out.println("\n" + "==================" + "\n");

                    for (PlaceDescriptionDto placeDescription : PlaceDescriptionList) {
                        System.out.println(placeDescription);
                        System.out.println("---------------");
                    }

                    return null;
                }
        );

        return combineFuture;
    }

    private FindAreaDto findArea(String placeName) throws IOException {
        StringBuilder url = new StringBuilder("https://graphhopper.com/api/1/geocode?");
        url.append("q=" + placeName);
        url.append("&locale=en");
        url.append("&key=" + property.getProperty("graphhopper"));

        Response response = GetResponse.createRequestAndReturnResponse(url.toString());
        return  (FindAreaDto) CreateDto.create(response, FindAreaDto.class);
    }

    private WeatherDto getWeatherByPoint(Point point) throws IOException {
        StringBuilder url = new StringBuilder("https://api.openweathermap.org/data/2.5/weather?");
        url.append("lat=" + point.getLat());
        url.append("&lon=" + point.getLng());
        url.append("&units=metric");
        url.append("&appid=" + property.getProperty("weather"));

        Response response = GetResponse.createRequestAndReturnResponse(url.toString());
        return (WeatherDto) CreateDto.create(response, WeatherDto.class);
    }

    private PlaceDto findPlaces(Point point, int radius) throws IOException {
        StringBuilder url = new StringBuilder("https://api.opentripmap.com/0.1/ru/places/radius?");
        url.append("&lat=" + point.getLat());
        url.append("&lon=" + point.getLng());
        url.append("&radius=" + radius);
        url.append("&lang=en");
        url.append("&limit=10");
        url.append("&apikey=" + property.getProperty("OpenTripPlanner"));

        Response response = GetResponse.createRequestAndReturnResponse(url.toString());
        return (PlaceDto) CreateDto.create(response, PlaceDto.class);
    }

    private PlaceDescriptionDto findPlaceDescription(String xid) throws IOException {
        StringBuilder url = new StringBuilder("https://api.opentripmap.com/0.1/ru/places/xid/");
        url.append(xid);
        url.append("?apikey=" + property.getProperty("OpenTripPlanner"));

        Response response = GetResponse.createRequestAndReturnResponse(url.toString());
        return (PlaceDescriptionDto) CreateDto.create(response, PlaceDescriptionDto.class);
    }

    @Data @Setter
    @AllArgsConstructor @NoArgsConstructor
    private class PlaceAndHisDescription {

        private PlaceDto placeDto;

        private PlaceDescriptionDto placeDescriptionDto;
    }
}
