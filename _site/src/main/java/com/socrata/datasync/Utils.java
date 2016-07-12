package com.socrata.datasync;

import au.com.bytecode.opencsv.CSVReader;
import com.socrata.datasync.config.controlfile.FileTypeControl;

import java.awt.Desktop;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static final String BOM = "\uFEFF";

    /**
     * Get file extension from the given path to a file
     * @param file filename
     * @return
     */
    public static String getFileExtension(String file) {
        String extension = "";
        int i = file.lastIndexOf('.');
        if (i > 0)
            extension = file.substring(i+1).toLowerCase();
        return extension;
    }

    public static String getFilename(String path) {
        return Paths.get(path).getFileName().toString();
    }

    /**
     * Returns a random 32 character request id
     */
    public static String generateRequestId() {
        String uuid = UUID.randomUUID().toString();
        String requestId = uuid.replace("-", "");
        return requestId;
    }

    public static String capitalizeFirstLetter(String s) {
        return s.substring(0, 1).toUpperCase()
                + s.substring(1);
    }

    /**
     * @param uid to validate
     * @return true if given uid is a valid Socrata uid (e.g. abcd-1234)
     */
    public static boolean uidIsValid(String uid) {
        Matcher uidMatcher = Pattern.compile("[a-z0-9]{4}-[a-z0-9]{4}").matcher(uid);
        return uidMatcher.matches();
    }

    /**
     * Reads first line of the given file after skipping 'skip' lines and returns it's contents as a string array.
     */
    public static String[] pullHeadersFromFile(File fileToPublish, FileTypeControl fileControl, int skip)
            throws IOException {

        CSVReader reader = getReader(fileToPublish, fileControl);

        int linesRead = 0;
        String[] nextRecord;
        Charset charset = getCharset(fileControl);
        while ((nextRecord = reader.readNext()) != null && linesRead++ < skip) {}
        byte[] bom = BOM.getBytes(charset);
        if (nextRecord != null && nextRecord.length > 0) {
            byte[] firstStringBytes = nextRecord[0].getBytes(charset);
            if (startsWith(bom, firstStringBytes))
                nextRecord[0] = nextRecord[0].substring(1);
        }
        reader.close();
        return nextRecord;
    }

    public static Charset getCharset(FileTypeControl fileControl) {
        Charset charset;
        try {
            charset = Charset.forName(fileControl.encoding);
        } catch (Exception e) {
            charset = Charset.defaultCharset();
        }
        return charset;
    }

    /** returns whether the source array starts with the prefix array
     * @param prefix the byte array containing the prefix
     * @param source the byte array of the source
     */
    private static boolean startsWith(byte[] prefix, byte[] source) {
        if (prefix.length > source.length) {
            return false;
        } else {
            for (int i = 0; i < prefix.length; i++) {
                if (prefix[i] != source[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Reads first line of the given file and determines whether the UTF-8 bom marker is present.
     */
    public static boolean fileStartsWithBom(File fileToPublish, FileTypeControl fileControl) throws IOException {
        Charset charset = getCharset(fileControl);
        if (charset.newEncoder().canEncode('\uffef')) {
            byte bom[] = BOM.getBytes(charset);
            FileInputStream is = null;
            try {
                is = new FileInputStream(fileToPublish);
                byte startingBytes[] = new byte[bom.length];
                int result = is.read(startingBytes);
                if (result != -1) {
                    return startsWith(bom, startingBytes);
                } else {
                    return false;
                }
            } finally {
                if (is != null)
                    is.close();
            }
        } else {
            return false;
        }
    }

    /**
     * Sets up a csvReader.
     */
    private static CSVReader getReader(File fileToPublish, FileTypeControl fileControl)
            throws IOException {

        String separator = fileControl.separator;
        if (separator == null)
            separator = Utils.getFileExtension(fileToPublish.getName()).equals("csv") ? "," : "\t";

        String quote = fileControl.quote == null ? "\"" : fileControl.quote;
        String escape = fileControl.escape == null ? "\u0000" : fileControl.escape;

        return new CSVReader(new FileReader(fileToPublish), separator.charAt(0), quote.charAt(0), escape.charAt(0), 0);
    }


    /**
     * Open given uri in local web browser
     * @param uri to open in browser
     */
    public static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                System.out.println("Error: cannot open web page");
            }
        }
    }

    /**
     * @param pathToSaveJobFile path to a saved job file
     * @return command with absolute paths to execute job file at given path
     */
    public static String getRunJobCommand(String pathToSaveJobFile) {
        String jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            jarPath = URLDecoder.decode(jarPath, "UTF-8");
            // Needed correct issue with windows where path includes a leading slash
            if(jarPath.contains(":") && (jarPath.startsWith("/") || jarPath.startsWith("\\"))) {
                jarPath = jarPath.substring(1, jarPath.length());
            }
            //TODO: This may change based on how we implement running metadata jobs from the command line.
            return "java -jar \"" + jarPath + "\" \"" + pathToSaveJobFile + "\"";
        } catch (UnsupportedEncodingException unsupportedEncoding) {
            return "Error getting path to this executeable: " + unsupportedEncoding.getMessage();
        }
    }

    public static int readChunk(InputStream in, byte[] buffer, int offset, int length) throws IOException {
        // InputStream.read isn't guaranteed to read all the bytes requested in one go.
        int initialOffset = offset;
        while(length > 0) {
            int count = in.read(buffer, offset, length);
            if(count == -1) break;
            offset += count;
            length -= count;
        }
        if(offset == initialOffset && length != 0) return -1;
        return offset - initialOffset;
    }

    public static String ordinal(int i) {
        return i % 100 == 11 || i % 100 == 12 || i % 100 == 13 ? i + "th" : i + new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"}[i % 10];
    }

    public static boolean nullOrEmpty(String s) {
        return (s == null || s.equals(""));
    }

    public static String getUserAgentString(String agentName) {
        try {
            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");
            String javaVersion = System.getProperty("java.version");
            String userLocale = System.getProperty("user.country") + "-" + System.getProperty("user.language");
            return "DataSync/" + VersionProvider.getThisVersion() +
                    " (" + agentName + "; " + osName + " " + osVersion + "; Java " + javaVersion + "; " + userLocale + ")";
        } catch (Exception e) {
            return "DataSync/" + VersionProvider.getThisVersion() +
                    " (" + agentName + "; Error obtaining OS/Java/Locale info)";
        }
    }
}
