package com.socrata.datasync.publishers;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.ImmutableMap;
import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.datasync.PublishMethod;
import com.socrata.datasync.Utils;
import com.socrata.exceptions.SodaError;
import com.socrata.model.UpsertError;
import com.socrata.model.UpsertResult;
import com.socrata.model.importer.Column;
import com.socrata.model.importer.Dataset;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Soda2Publisher {

    private Soda2Publisher() {
        throw new AssertionError("Never instantiate utility classes!");
    }

    /**
     * Deletes the rows corresponding to the IDs within the given file
     * @param csvOrTsvFile a file specifiying the ids of the rows to be deleted
     * @return an upsertResult with the number of rows deleted and the number of errors encountered=
     */
    public static UpsertResult deleteRows(Soda2Producer producer, SodaDdl ddl,
                                          final String id, final File csvOrTsvFile, final int numRowsPerChunk, final boolean containsHeaderRow)
            throws IOException, SodaError, InterruptedException
    {
        List<Map<String, Object>> upsertObjectsChunk = new ArrayList<Map<String, Object>>();
        int totalRowsDeleted = 0;
        List<UpsertError> deleteErrors = new ArrayList<UpsertError>();

        String rowIdentifierName = getDatasetRowId(ddl, id);

        FileReader fileReader = new FileReader(csvOrTsvFile);
        CSVReader reader = new CSVReader(fileReader);
        String[] currLine;

        // skip first row if there is a header row
        if(containsHeaderRow) {
            reader.readNext();
        }

        do {
            currLine = reader.readNext();
            if(currLine != null) {
                upsertObjectsChunk.add(ImmutableMap.of(rowIdentifierName, (Object) currLine[0], ":deleted", Boolean.TRUE));
            }
            if(upsertObjectsChunk.size() == numRowsPerChunk || currLine == null) {
                UpsertResult chunkResult = producer.upsert(id, upsertObjectsChunk);
                totalRowsDeleted += chunkResult.getRowsDeleted();

                if(chunkResult.errorCount() > 0) {
                    // TODO find a better way to suppress these errors (which are really not errors anyway)
                    for(UpsertError err : chunkResult.getErrors()) {
                        if(!err.getError().contains("no record is found")) {
                            deleteErrors.add(err);
                        }
                    }
                }
                upsertObjectsChunk.clear();
            }
        } while(currLine != null);
        reader.close();

        return new UpsertResult(
                0, 0, totalRowsDeleted, deleteErrors);
    }


    /**
     *
     * Publishes the given csvOrTsvFile via SODA 2 replace or upsert/append. Publishes in chunks if using
     * upsert/append and numRowsPerChunk > 0 (all data will be upserted in one chunk if numRowsPerChunk == 0)
     * where each chunk contains numRowsPerChunk rows. Chunking is useful when uploading very large CSV files.
     *
     * @param method to use to publish (upsert, append, or replace; delete not allowed)
     * @param id dataset ID to publish to
     * @param csvOrTsvFile file to publish containing data in comma- or tab- separated values (CSV or TSV) format
     * @param numRowsPerChunk number of rows within CSV to publish in each chunk
     *                        (if numRowsPerChunk == 0 do not use chunking)
     * @param containsHeaderRow if true assume the first row in CSV/TSV file is a list of the dataset columns,
     *                          otherwise upload all rows as new rows (column order must exactly match that of
     *                          Socrata dataset)
     * @return UpsertResult containing success or error information
     * @throws java.io.IOException
     * @throws com.socrata.exceptions.SodaError
     * @throws InterruptedException
     */
    public static UpsertResult publishViaSoda2(Soda2Producer producer, SodaDdl ddl,
                                               final PublishMethod method, final String id, final File csvOrTsvFile,
                                               int numRowsPerChunk, final boolean containsHeaderRow)
            throws IOException, SodaError, InterruptedException
    {
        // If doing a replace force it to upload all data as a single chunk
        if(method.equals(PublishMethod.replace)) {
            System.out.println("WARNING: replace does not support chunking.");
            numRowsPerChunk = 0;
        }

        List<Map<String, Object>> upsertObjectsChunk = new ArrayList<Map<String, Object>>();
        int totalRowsCreated = 0;
        int totalRowsUpdated = 0;
        int totalRowsDeleted = 0;
        List<UpsertError> upsertErrors = new ArrayList<UpsertError>();

        char columnDelimiter = ',';
        if(Utils.getFileExtension(csvOrTsvFile.toString()).equals("tsv")) {
            columnDelimiter = '\t';
        }

        int numUploadedChunks = 0;
        FileReader  fileReader = new FileReader(csvOrTsvFile);
        CSVReader reader = new CSVReader(fileReader, columnDelimiter);

        String[] headers;
        if(containsHeaderRow) {
            headers = reader.readNext();
            // trim whitespace from header names
            if (headers != null) {
                for (int i=0; i<headers.length; i++) {
                    headers[i] = headers[i].trim();
                }
            }
        } else {
            // get API field names for each column in dataset
            Dataset info = (Dataset) ddl.loadDatasetInfo(id);
            List<Column> columns = info.getColumns();
            headers = new String[columns.size()];
            for(int i = 0; i < columns.size(); i++) {
                headers[i] = columns.get(i).getFieldName();
            }
        }

        if (headers != null) {
            String[] currLine;
            do {
                currLine = reader.readNext();
                ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
                if(currLine != null) {
                    for (int i=0; i<currLine.length; i++) {
                        if (i < headers.length) {
                            builder.put(headers[i], currLine[i]);
                        }
                    }
                    upsertObjectsChunk.add(builder.build());
                }
                if(upsertObjectsChunk.size() == numRowsPerChunk || currLine == null) {
                    if(numRowsPerChunk == 0) {
                        System.out.println("Publishing entire file via HTTP...");
                    } else {
                        System.out.println("Publishing file in chunks via HTTP (" + numUploadedChunks * numRowsPerChunk + " rows uploaded so far)...");
                    }

                    // upsert or replace current chunk
                    UpsertResult chunkResult;
                    if(method.equals(PublishMethod.upsert) || method.equals(PublishMethod.append)) {
                        chunkResult = producer.upsert(id, upsertObjectsChunk);
                    } else if(method.equals(PublishMethod.replace)) {
                        chunkResult = producer.replace(id, upsertObjectsChunk);
                    } else {
                        reader.close();
                        throw new IllegalArgumentException("Error performing publish: "
                                + method + " is not a valid publishing method");
                    }
                    totalRowsCreated += chunkResult.getRowsCreated();
                    totalRowsUpdated += chunkResult.getRowsUpdated();
                    totalRowsDeleted += chunkResult.getRowsDeleted();
                    numUploadedChunks += 1;

                    if(chunkResult.errorCount() > 0) {
                        if(numRowsPerChunk != 0) {
                            for (UpsertError upsertErr : chunkResult.getErrors()) {
                                int lineIndexOffset = (containsHeaderRow) ? 2 : 1;
                                System.err.println("Error uploading chunk " + numUploadedChunks + ": " +
                                        upsertErr.getError() + " (line " +
                                        (upsertErr.getIndex() + lineIndexOffset + ((numUploadedChunks-1) * numRowsPerChunk)) + " of file)");
                            }
                        }
                        upsertErrors.addAll(chunkResult.getErrors());
                    }

                    if(numRowsPerChunk != 0) {
                        System.out.println("Chunk " + numUploadedChunks + " uploaded: " + chunkResult.getRowsCreated() + " rows created; " +
                                chunkResult.getRowsUpdated() + " rows updated; " + chunkResult.getRowsDeleted() +
                                " rows deleted; " + chunkResult.errorCount() + " rows omitted");
                    }

                    upsertObjectsChunk.clear();
                }
            } while(currLine != null);
        }
        reader.close();
        return new UpsertResult(
                totalRowsCreated, totalRowsUpdated, totalRowsDeleted, upsertErrors);
    }


    /**
     * This operation will do an append/upsert.
     *
     * IMPORTANT: If you have a row identifier set on the dataset, and this is appending a row that
     * has an identifier of a row that has already been added, this will overwrite that row rather than
     * failing. If you have no row identifier set, this will be a straight append every time.
     */
    public static UpsertResult appendUpsert(Soda2Producer producer, SodaDdl ddl,
                                            final String id, final File file,
                                            int numRowsPerChunk, boolean containsHeaderRow)
            throws SodaError, InterruptedException, IOException
    {
        return publishViaSoda2(producer, ddl, PublishMethod.upsert, id, file, numRowsPerChunk, containsHeaderRow);
    }


    /**
     * This is a new replace function that does not need a working copy.
     */
    public static UpsertResult replaceNew(Soda2Producer producer, SodaDdl ddl,
                                          final String id, final File file,
                                          boolean containsHeaderRow)
            throws SodaError, InterruptedException, IOException {
        return publishViaSoda2(producer, ddl, PublishMethod.replace, id, file, 0, containsHeaderRow);
    }

    private static String getDatasetRowId(SodaDdl ddl, String id) throws SodaError, InterruptedException {
        Dataset info = (Dataset) ddl.loadDatasetInfo(id);
        Column rowIdentifier = info.lookupRowIdentifierColumn();
        String rowIdentifierName;
        if (rowIdentifier == null) {
            rowIdentifierName = ":id";
        } else {
            rowIdentifierName = rowIdentifier.getFieldName();
        }
        return rowIdentifierName;
    }
}

