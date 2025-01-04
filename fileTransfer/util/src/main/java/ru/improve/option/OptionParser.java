package ru.improve.option;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class OptionParser {

    private static OptionParser optionParser;

    private static Options options;

    private OptionParser() {
        options = new Options();

        //server
        options.addOption("p", "port", true, "listen port");

        //client
        options.addOption("i", "ip", true, "server ip address");
        options.addOption("f", "filePath", true, "path to file");
    }

    public static OptionParser getInstance() {
        if (optionParser == null) {
            optionParser = new OptionParser();
        }

        return optionParser;
    }

    public static CommandLine parse(String[] args) throws ParseException {
        CommandLine cmd;
        cmd = new DefaultParser().parse(options, args);
        return cmd;
    }

}
