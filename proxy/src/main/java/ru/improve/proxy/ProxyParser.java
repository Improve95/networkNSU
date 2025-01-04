package ru.improve.proxy;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ProxyParser {
    private final Options options;

    public ProxyParser() {
        options = new Options();
        options.addOption("p", "port", true, "Socket listen in this port.");
    }

    public int parse(String[] args) {
        CommandLine cli;
        try {
            cli = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        if (cli.hasOption("p")) {
            return Integer.parseInt(cli.getOptionValue("p"));
        }
        else {
            throw new RuntimeException("Usage: -p <port>");
        }
    }

}
