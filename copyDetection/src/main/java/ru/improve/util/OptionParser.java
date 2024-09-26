package ru.improve.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class OptionParser {

    private Options options = new Options();

    public OptionParser() {
        options.addOption("i", "ip", true, "group ip address");
        options.addOption("p", "post", true, "port");
        options.addOption("k", "key", true, "sender key");
    }

    public CommandLine parse(String[] args) throws ParseException {
        CommandLine cmd;
        cmd = new DefaultParser().parse(options, args);
        return cmd;
    }
}
