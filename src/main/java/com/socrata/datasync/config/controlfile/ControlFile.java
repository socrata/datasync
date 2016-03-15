package com.socrata.datasync.config.controlfile;

import com.socrata.datasync.PublishMethod;
import com.socrata.datasync.Utils;
import com.socrata.datasync.job.IntegrationJob;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.HashMap;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonPropertyOrder(alphabetic=true)
public class ControlFile {

    public String action;
    public String opaque;

    public FileTypeControl csv;
    public FileTypeControl tsv;
    public Boolean replacePreviousQueued;

    // NB: when using a mapper to read this class, you must enable
    // DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, if either of the timestamp formats
    // in csvControl or tsvControl are strings, rather than arrays of strings.
    public ControlFile() {}

    public ControlFile(String action, String opaque, FileTypeControl csvControl, FileTypeControl tsvControl, Boolean replacePreviousQueued) {
        this.action = action;
        this.opaque = opaque;
        this.csv = csvControl;
        this.tsv = tsvControl;

    }


    // Hack city.  Apparently the ControlFile must have both a csv and a tsv control file, even though only one may be
    // active at any given time.  The control file editor will be manipulating the active one, regardless of type.
    // Thus, I need to return the active FTC here that can later be manipulated throughout the model.
    @JsonIgnore
    public FileTypeControl getFileTypeControl(){
        if (csv != null)
            return csv;
        else
            return tsv;
    }

    @JsonIgnore
    public FileTypeControl getCsvFtc(){
        return csv;
    }

    @JsonIgnore
    public FileTypeControl getTsvFtc(){
        return tsv;
    }


    /**
    * Generates the default ControlFile object based on given job parameters
    *
    * @param fileToPublish filename of file to publish (.tsv or .csv file)
    * @param publishMethod to use to publish (upsert, append, replace, or delete)
    *               NOTE: this option will be overriden if this control file is for an ftp job
    * @param columns the column headers correcsponding to the filetoPublish, needed if it lacks headers
    * @param useSocrataGeocoding if true use Socrata's geocoding to geocode Location columns
    * @return content of control.json based on given job parameters
    */
    public static ControlFile generateControlFile(final String fileToPublish,
                                                  final PublishMethod publishMethod,
                                                  final String[] columns,
                                                  final boolean useSocrataGeocoding,
                                                    final boolean hasHeaderRow) {


        String fileToPublishExtension = Utils.getFileExtension(fileToPublish);


        boolean isCsv = fileToPublishExtension.equalsIgnoreCase("csv");
        String quote = isCsv ? "\"" : "\u0000";

        FileTypeControl ftc = new FileTypeControl()
                .columns(columns)
                .encoding("utf-8")
                .hasHeaderRow(hasHeaderRow)
                .quote(quote);


        if (!PublishMethod.delete.equals(publishMethod)) {
            int skip = 0;
            String separator = isCsv ? "," : "\t";
            //Adding our standard export formats so that a customer can easily round-trip data into the system.
            String[] timeFormats = new String[]{"ISO8601", "MM/dd/yy", "MM/dd/yyyy", "dd-MMM-yyyy","MM/dd/yyyy HH:mm:ss a Z","MM/dd/yyyy HH:mm:ss a"};
            ftc.emptyTextIsNull(true)
               .filePath(fileToPublish)
               .ignoreColumns(new String[]{})
               .fixedTimestampFormat(timeFormats)
               .floatingTimestampFormat(timeFormats)
               .separator(separator)
               .skip(skip)
               .timezone("UTC")
               .useSocrataGeocoding(useSocrataGeocoding)
               .trimWhitespace(true)
               .trimServerWhitespace(true)
               .overrides(new HashMap<String, ColumnOverride>())
               .setAsideErrors(false);


            // for replace jobs, calculate column statistics
            if (PublishMethod.replace.equals(publishMethod))
                ftc.columnStatistics(true);

        }

        if (isCsv) {
            return new ControlFile(Utils.capitalizeFirstLetter(publishMethod.name()), null, ftc, null, null);
        } else {
            return new ControlFile(Utils.capitalizeFirstLetter(publishMethod.name()), null, null, ftc, null);
        }
    }

    public String generateAndAddOpaqueUUID() {
        String uuid = Utils.generateRequestId();
        this.opaque = uuid;
        return uuid;
    }
}

