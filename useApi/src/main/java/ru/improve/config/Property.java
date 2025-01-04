package ru.improve.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Property {

    private static Properties properties;

    public static Properties getInstance() {
        if (properties != null) {
            return properties;
        }

        try (InputStream inputStream = Property.class.getClassLoader().getResourceAsStream("keys.properties")) {
            properties = new Properties();
            properties.load(inputStream);
        } catch (IOException ex) {
            throw new RuntimeException("cannot open properties file");
        }

        return properties;
    }
}
