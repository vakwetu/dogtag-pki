package com.netscape.cmstools.key;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import com.netscape.certsrv.base.ResourceMessage;
import com.netscape.certsrv.key.KeyArchivalRequest;
import com.netscape.cmstools.cli.CLI;
import com.netscape.cmstools.cli.MainCLI;

public class KeyTemplateShowCLI extends CLI {
    public KeyCLI keyCLI;

    public KeyTemplateShowCLI(KeyCLI keyCLI) {
        super("template-show", "Get request template", keyCLI);
        this.keyCLI = keyCLI;

        createOptions();
    }

    public void printHelp() {
        formatter.printHelp(getFullName() + " <Template ID> [OPTIONS...]", options);
    }

    public void createOptions() {
        Option option = new Option(null, "output", true, "Location to store the template.");
        option.setArgName("output file");
        options.addOption(option);
    }

    public void execute(String[] args) {
        // Always check for "--help" prior to parsing
        if (Arrays.asList(args).contains("--help")) {
            // Display usage
            printHelp();
            System.exit(0);
        }

        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            printHelp();
            System.exit(-1);
        }

        String[] cmdArgs = cmd.getArgs();

        if (cmdArgs.length < 1) {
            System.err.println("Error: No Template ID specified.");
            printHelp();
            System.exit(-1);
        }

        String templateId = cmdArgs[0];
        String writeToFile = cmd.getOptionValue("output");
        String templateDir = "/usr/share/pki/key/templates/";
        String templatePath = templateDir + templateId + ".xml";
        ResourceMessage data = null;
        try {
            data = ResourceMessage.unmarshall(KeyArchivalRequest.class, templatePath);
        } catch (FileNotFoundException | JAXBException e2) {
            System.err.println("Error: " + e2.getMessage());
            if (verbose)
                e2.printStackTrace();
            System.exit(-1);
        }

        if (writeToFile != null) {
            try (FileOutputStream fOS = new FileOutputStream(writeToFile)) {
                data.marshall(fOS);
            } catch (JAXBException e) {
                System.err.println("Error: Cannot write the file");
                if (verbose)
                    e.printStackTrace();
            } catch (FileNotFoundException e) {
                System.err.println("Error: Cannot write the file");
                if (verbose)
                    e.printStackTrace();
            } catch (IOException e1) {
                System.err.println("Error: " + e1.getMessage());
                if (verbose)
                    e1.printStackTrace();
            }
        } else {
            MainCLI.printMessage(data.getAttribute("description"));
            try {
                data.marshall(System.out);
            } catch (JAXBException e) {
                System.err.println(e.getMessage());
                if (verbose)
                    e.printStackTrace();
            }
        }
    }
}
