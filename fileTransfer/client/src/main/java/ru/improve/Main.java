package ru.improve;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import ru.improve.client.Client;
import ru.improve.option.OptionParser;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        OptionParser optionParser = OptionParser.getInstance();

        CommandLine cmd;
        try {
            cmd = optionParser.parse(args);
        } catch (ParseException ex) {
            throw new RuntimeException(ex.getMessage());
        }

        String ip = cmd.getOptionValue("i");
        int port = Integer.parseInt(cmd.getOptionValue("p"));
        String filePath = cmd.getOptionValue("f");

        if (ip.isEmpty()) throw new RuntimeException("ip is empty");
        if (filePath.isEmpty()) throw new RuntimeException("file path is empty");

        Client client = new Client();
        client.sendFile(ip, port, filePath);

        Scanner scanner = new Scanner(System.in);
        while (!scanner.next().equals("q"));

        client.stop();
    }
}