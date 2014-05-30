package com.socrata.datasync.utilities;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.socrata.api.HttpLowLevel;
import com.socrata.api.Soda2Consumer;
import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.datasync.JobStatus;
import com.socrata.datasync.PublishMethod;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import com.socrata.model.importer.Column;
import com.socrata.model.importer.Dataset;
import com.socrata.model.importer.DatasetInfo;
import com.socrata.model.soql.SoqlQuery;
import com.sun.jersey.api.client.ClientResponse;

public class PortUtility {

    private static final String groupingKey = "grouping_aggregate";
    private static final String drillingKey = "drill_down";

	private PortUtility() {
		throw new AssertionError("Never instantiate utility classes!");
	}

	public static String portSchema(SodaDdl loader, SodaDdl creator,
			final String sourceSetID, final String destinationDatasetTitle,
            final boolean useNewBackend) throws SodaError, InterruptedException {
		Dataset sourceSet = (Dataset) loader.loadDatasetInfo(sourceSetID);
        if(destinationDatasetTitle != null && !destinationDatasetTitle.equals(""))
            sourceSet.setName(destinationDatasetTitle);

        adaptSchemaForAggregates(sourceSet);

        // TODO uncomment (after soda-java is updated to support this)
		//DatasetInfo sinkSet = creator.createDataset(sourceSet, useNewBackend);
        DatasetInfo sinkSet = creator.createDataset(sourceSet);

        String sinkSetID = sinkSet.getId();
		return sinkSetID;
	}

	public static String publishDataset(SodaDdl publisher, String sinkSetID)
			throws SodaError, InterruptedException {
		DatasetInfo publishedSet = publisher.publish(sinkSetID);
		String publishedID = publishedSet.getId();
		return publishedID;
	}

	public static void portContents(Soda2Consumer streamExporter,
			Soda2Producer streamUpserter, String sourceSetID, String sinkSetID,
			PublishMethod publishMethod) throws InterruptedException {
		// Limit of 1000 rows per export, so offset "pages" through dataset
		// 1000 at a time
		int offset = 0;
		// Initialize response object (we get the stream from this later)
		ClientResponse response = null;
		// The stream
		String sourceSetData = null;
		do {
			// Can't query dataset semaphore to my knowledge, so wait 1000
			// milliseconds (1 second)
			Thread.sleep(1000);
			// SoqlQuery has multiple parameters, the only important one for
			// us is offset
			SoqlQuery myQuery = new SoqlQuery(null, null, null, null, null,
					null, offset, null);
			try {
				try {
					// Query using Soda2Consumer object
					response = streamExporter.query(sourceSetID,
							HttpLowLevel.JSON_TYPE, myQuery);
				} catch (SodaError sodaError) {
					System.out.println("SODA error: " + sodaError.getMessage());
				}
			} catch (LongRunningQueryException e) {
				System.out.println("Query too long to run: " + e.getMessage());
			}
			// Convert the ClientResponse object to String
			sourceSetData = response.getEntity(String.class).trim();
			// Increment the offset
			offset += 1000;
			// Magic number (I'm so sorry)... an "empty" response has a
			// length of 3.
			// If the response is empty, do not upsert.
			if (sourceSetData.length() > 3) {
				try {
					// Convert String to byte array
					InputStream sourceSetStream = new ByteArrayInputStream(
							sourceSetData.getBytes("UTF-8"));
					// Upsert or replace using Soda2Producer object
					if (publishMethod.equals(PublishMethod.upsert)) {
						streamUpserter.upsertStream(sinkSetID,
								HttpLowLevel.JSON_TYPE, sourceSetStream);
					} else if (publishMethod.equals(PublishMethod.replace)){
						streamUpserter.replaceStream(sinkSetID,
								HttpLowLevel.JSON_TYPE, sourceSetStream);
					}
				} catch (SodaError sodaError) {
					System.out.println(sodaError.getMessage());
				} catch (Exception exception) {
					System.out.println(exception.getMessage());
				}
			}
			// Break after one empty response.
		} while (sourceSetData.length() > 3);
	}

    public static JobStatus assertSchemasAreAlike(SodaDdl sourceChecker, SodaDdl sinkChecker, String sourceSetID, String sinkSetID) throws SodaError, InterruptedException {
        // We don't need to test metadata; we're only concerned with the columns...
        Dataset sourceSchema = (Dataset) sourceChecker.loadDatasetInfo(sourceSetID);
        Dataset sinkSchema = (Dataset) sinkChecker.loadDatasetInfo(sinkSetID);
        // Grab the columns...
        List<Column> sourceColumns = sourceSchema.getColumns();
        List<Column> sinkColumns = sinkSchema.getColumns();
        // And let the tests begin.
        if(sourceColumns.size() == sinkColumns.size()) {
            // If the sizes are the same we can begin comparing columns
            for (int i = 0; i < sourceColumns.size(); i++) {
                // The aspects of the columns that we care about are the API field names and their data types
                if(!sourceColumns.get(i).getFieldName().equals(sinkColumns.get(i).getFieldName()) ||
                        !sourceColumns.get(i).getDataTypeName().equals(sinkColumns.get(i).getDataTypeName())){
                    return JobStatus.INVALID_SCHEMAS;
                }
            }
        } else {
                return JobStatus.INVALID_SCHEMAS;
        }
        return JobStatus.SUCCESS;
    }

    /**
     * Changes the columnar information in the given dataset to remove the "grouping_aggregate" field from "format",
     * when present, and prepend its value to the column field name.  Removal of the field is necessary to
     * successfully upload the schema to core (which would otherwise throw an error about refusing to create a column
     * with a grouping_aggregrate but no group-by).  The editing of the field name is necessary for subsequent
     * data loading, since the data from soda2 expectst aggregated columns to include the grouping_aggregate.
     * Also removes drill-down formatting info, as this is non-sensical without the unaggregated data
     * @param schema the Dataset from soda-java representing the schema
     */
     public static void adaptSchemaForAggregates(Dataset schema) {
        List<Column> columns = schema.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            Column col = columns.get(i);
            if (col != null) {
                Map<String, String> format = col.getFormat();
                if (format != null) {
                    String aggregation = format.remove(groupingKey);
                    format.remove(drillingKey);
                    if (aggregation != null) {
                        String oldFieldName = col.getFieldName();
                        col.setFieldName(aggregation + "_" + oldFieldName);
                    }
                }
            }
        }
    }
}
