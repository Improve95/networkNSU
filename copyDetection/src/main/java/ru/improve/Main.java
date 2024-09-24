package ru.improve;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import ru.improve.multicast.Multicast;
import ru.improve.util.OptionParser;

public class Main {
    public static void main(String[] args) {

        OptionParser optionParser = new OptionParser();

        CommandLine cmd;
        try {
            cmd = optionParser.parse(args);
        } catch (ParseException ex) {
            throw new RuntimeException(ex.getMessage());
        }

        Multicast
    }
}