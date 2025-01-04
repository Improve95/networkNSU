package ru.improve;

import ru.improve.place.FindPlace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Main {

    public static void main(String[] args) {
        try (InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("userInput.txt");
                BufferedReader bf = new BufferedReader(new InputStreamReader(inputStream))) {

            FindPlace findPlace = new FindPlace(bf);
            CompletableFuture<Void> findPlaceFuture = findPlace.find();

            findPlaceFuture.get();

        } catch (IOException | InterruptedException | ExecutionException ex) {
            throw  new RuntimeException(ex.getMessage());
        }
    }
}