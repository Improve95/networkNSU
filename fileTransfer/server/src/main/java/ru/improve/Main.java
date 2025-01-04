package ru.improve;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import ru.improve.server.Server;
import ru.improve.model.ServerModel;
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

        int port = Integer.parseInt(cmd.getOptionValue("p"));

        ServerModel serverModel = new ServerModel();

        Server server = new Server(serverModel);
        server.initial(port);

        Scanner scanner = new Scanner(System.in);
        while (!scanner.nextLine().equals("q"));

        server.stop();
    }
}