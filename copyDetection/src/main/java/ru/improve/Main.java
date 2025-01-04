package ru.improve;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import ru.improve.multicast.CopyDetection;
import ru.improve.util.OptionParser;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        OptionParser optionParser = new OptionParser();

        CommandLine cmd;
        try {
            cmd = optionParser.parse(args);
        } catch (ParseException ex) {
            throw new RuntimeException(ex.getMessage());
        }

        String groupIpAddress = cmd.getOptionValue("i");

        int port;
        try {
            port = Integer.parseInt(cmd.getOptionValue("p"));
        } catch (NumberFormatException ex) {
            throw new RuntimeException("bad port");
        }

        String key;
        if ((key = cmd.getOptionValue("k")).isEmpty()) {
            throw new RuntimeException("empty send key");
        }

        CopyDetection copyDetection = new CopyDetection();
        copyDetection.start(groupIpAddress, port, key);

        String line;
        Scanner scanner = new Scanner(System.in);
        do {
            line = scanner.next();
            copyDetection.stop();
        } while (!line.equals("q"));
    }
}