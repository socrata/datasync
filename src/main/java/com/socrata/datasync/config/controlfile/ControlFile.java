package com.socrata.datasync.config.controlfile;

import com.socrata.datasync.utilities.IntegrationUtility;
import com.socrata.datasync.PublishMethod;
import com.socrata.exceptions.SodaError;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.IOException;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonPropertyOrder(alphabetic=true)
public class ControlFile {

    public String action;
    public FileTypeControl csv;
    public FileTypeControl tsv;

    public ControlFile() {}

    public ControlFile(String action, FileTypeControl csvControl, FileTypeControl tsvControl) {
        this.action = action;
        this.csv = csvControl;
        this.tsv = tsvControl;
    }


    /**
     * Generates the default ControlFile object based on given job parameters
     *
     * @param fileToPublish filename of file to publish (.tsv or .csv file)
     * @param publishMethod to use to publish (upsert, append, replace, or delete)
     *               NOTE: this option will be overriden if userPrefs has pathToFTPControlFile set
     * @param columns the column headers correcsponding to the filetoPublish, needed if it lacks headers
     * @param useSocrataGeocoding if true use Socrata's geocoding to geocode Location columns
     * @return content of control.json based on given job parameters
     * @throws com.socrata.exceptions.SodaError
     * @throws InterruptedException
     */
    public static ControlFile generateControlFile(final String fileToPublish,
                                                    final PublishMethod publishMethod,
                                                    final String[] columns,
                                                    final boolean useSocrataGeocoding) throws
            SodaError, InterruptedException, IOException {

        int skip = 0;
        String fileToPublishExtension = IntegrationUtility.getFileExtension(fileToPublish);
        boolean isCsv = fileToPublishExtension.equalsIgnoreCase("csv");
        String separator = isCsv ? "," : "\t";
        String quote = isCsv ? "\"" : "\u0000";
        String[] timeFormats = new String[]{"ISO8601", "MM/dd/yy", "MM/dd/yyyy", "dd-MMM-yyyy"};

        FileTypeControl ftc = new FileTypeControl()
                .columns(columns)
                .emptyTextIsNull(true)
                .encoding("utf-8")
                .fixedTimestampFormat(timeFormats)
                .floatingTimestampFormat(timeFormats)
                .quote(quote)
                .separator(separator)
                .skip(skip)
                .timezone("UTC")
                .trimWhitespace(true)
                .trimServerWhitespace(true)
                .useSocrataGeocoding(useSocrataGeocoding);

        if (isCsv) {
            return new ControlFile(capitalizeFirstLetter(publishMethod), ftc, null);
        } else {
            return new ControlFile(capitalizeFirstLetter(publishMethod), null, ftc);
        }
    }

    private static String capitalizeFirstLetter(PublishMethod method) {
        return method.name().substring(0, 1).toUpperCase()
                + method.name().substring(1);
    }
}

