// --- BEGIN COPYRIGHT BLOCK ---
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
// (C) 2016 Red Hat, Inc.
// All rights reserved.
// --- END COPYRIGHT BLOCK ---

package com.netscape.cmstools.pkcs12;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.mozilla.jss.util.Password;

import com.netscape.cmstools.cli.CLI;
import com.netscape.cmstools.cli.MainCLI;

import netscape.security.pkcs.PKCS12;
import netscape.security.pkcs.PKCS12Util;

/**
 * @author Endi S. Dewata
 */
public class PKCS12CertAddCLI extends CLI {

    public PKCS12CertAddCLI(PKCS12CertCLI certCLI) {
        super("add", "Add certificate into PKCS #12 file", certCLI);

        createOptions();
    }

    public void printHelp() {
        formatter.printHelp(getFullName() + " <nickname> [OPTIONS...]", options);
    }

    public void createOptions() {
        Option option = new Option(null, "pkcs12-file", true, "PKCS #12 file");
        option.setArgName("path");
        options.addOption(option);

        option = new Option(null, "pkcs12-password", true, "PKCS #12 password");
        option.setArgName("password");
        options.addOption(option);

        option = new Option(null, "pkcs12-password-file", true, "PKCS #12 password file");
        option.setArgName("path");
        options.addOption(option);

        options.addOption(null, "new-file", false, "Create a new PKCS #12 file");
        options.addOption(null, "no-trust-flags", false, "Do not include trust flags");
        options.addOption(null, "no-key", false, "Do not include private key");
        options.addOption(null, "no-chain", false, "Do not include certificate chain");

        options.addOption("v", "verbose", false, "Run in verbose mode.");
        options.addOption(null, "debug", false, "Run in debug mode.");
        options.addOption(null, "help", false, "Show help message.");
    }

    public void execute(String[] args) throws Exception {

        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            printHelp();
            System.exit(-1);
        }

        if (cmd.hasOption("help")) {
            printHelp();
            System.exit(0);
        }

        if (cmd.hasOption("verbose")) {
            Logger.getLogger("org.dogtagpki").setLevel(Level.INFO);
            Logger.getLogger("com.netscape").setLevel(Level.INFO);
            Logger.getLogger("netscape").setLevel(Level.INFO);

        } else if (cmd.hasOption("debug")) {
            Logger.getLogger("org.dogtagpki").setLevel(Level.FINE);
            Logger.getLogger("com.netscape").setLevel(Level.FINE);
            Logger.getLogger("netscape").setLevel(Level.FINE);
        }

        String[] cmdArgs = cmd.getArgs();

        if (cmdArgs.length == 0) {
            System.err.println("Error: Missing certificate nickname.");
            printHelp();
            System.exit(-1);
        }

        String nickname = cmdArgs[0];

        String filename = cmd.getOptionValue("pkcs12-file");

        if (filename == null) {
            System.err.println("Error: Missing PKCS #12 file.");
            printHelp();
            System.exit(-1);
        }

        String passwordString = cmd.getOptionValue("pkcs12-password");

        if (passwordString == null) {

            String passwordFile = cmd.getOptionValue("pkcs12-password-file");
            if (passwordFile != null) {
                try (BufferedReader in = new BufferedReader(new FileReader(passwordFile))) {
                    passwordString = in.readLine();
                }
            }
        }

        if (passwordString == null) {
            System.err.println("Error: Missing PKCS #12 password.");
            printHelp();
            System.exit(-1);
        }

        Password password = new Password(passwordString.toCharArray());

        boolean newFile = cmd.hasOption("new-file");
        boolean includeTrustFlags = !cmd.hasOption("no-trust-flags");
        boolean includeKey = !cmd.hasOption("no-key");
        boolean includeChain = !cmd.hasOption("no-chain");

        try {
            PKCS12Util util = new PKCS12Util();
            util.setTrustFlagsEnabled(includeTrustFlags);

            PKCS12 pkcs12;

            if (newFile || !new File(filename).exists()) {
                // if new file requested or file does not exist, create a new file
                pkcs12 = new PKCS12();

            } else {
                // otherwise, add into the existing file
                pkcs12 = util.loadFromFile(filename, password);
            }

            // load the specified certificate
            util.loadCertFromNSS(pkcs12, nickname, includeKey, includeChain);
            util.storeIntoFile(pkcs12, filename, password);

        } finally {
            password.clear();
        }

        MainCLI.printMessage("Added certificate \"" + nickname + "\"");
    }
}
