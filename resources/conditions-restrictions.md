---
layout: with-sidebar
title: Conditions and Restrictions
bodyclass: homepage
---

DataSync enforces particular restrictions on both the format of the CSV, as well as the data types within the CSV, when updating a dataset.  No data is added until all the following conditions are met:

  - Every row in the CSV/TSV, including the header, must have the same number of columns. Unbalanced quotation marks almost always create unequal numbers of columns, so be certain to balance these.
  - If the dataset has a row id, that field must be provided in the CSV/TSV.
  - If the dataset does not have a row id, then all fields in the dataset must be provided in the CSV.
  - All columns specified in the header must be present in the dataset.
  - All values can be interpreted as the appropriate type.
  - If `syntheticLocation` is provided in the [control file]({{ site.baseurl }}/resources/ftp-control-config.html), the name of the column that is constructed must not conflict with one already in the CSV.  You can use the `ignoreColumns` option to ignore the one in the CSV.

### Datatype Restrictions

Data must be formatted as follows to pass validation:

| Datatype    | Restrictions/Notes
| ------------- | ------------------------------
| Text | If `emptyTextIsNull` is true, an empty cell will be converted to a SoQL "null" value.  Otherwise it will be stored as an empty text value.
| Formatted Text | Due to the complexity involved in analyzing formatted text, DataSync assumes that all formatted text columns represent changes.  Because of this datasets containing formatted text may take longer to ingress.
| Number | Must use only numerals plus a decimal point (period), possibly with scientific notation (e.g., 6.02e+23). There can be no commas grouping digits. This applies even if the dataset is in a locale that uses a comma as the decimal point and a period to group digits.
| Money | The currency symbol must not be present. Follows the same rules as Number.
| Percent | The percent symbol must not be present. 45% should be represented as 45 not 0.45.
| Date & Time | If the `floatingTimestampFormat` is "ISO8601" data must be in the following format:  yyyy-mm-ddTHH:mm:ss
| Date & Time (with timezone) | If the `fixedTimestampFormat` is "ISO8601" data must be in the following format:  yyyy-mm-ddTHH:mm:ssz, where "z" is a four-digit-plus-sign offset from UTC (e.g., "-0800") or "Z" (which is  a synonym for "+0000").
| Location | A human-readable US address with a (latitude, longitude) pair.  Example (Note that this must be all a single CSV value, and therefore quoted appropriately): "123 Main St. Mytown, YN 12345 (-123.4324235, 33.234546324)". The address, city, state, zip, and coordinate sub-parts are all optional.  If not provided, the system will guess about how to break the field into the location’s constituent parts.  Note that this parsing is often non-deterministic and <strong>may result in unexpected values in your location column</strong>.  If your data does not include the coordinate pair, your control file should set `useSocrataGeocoding` to "true" for optimal performance - this prevents unnecessary replacement of rows that are viewed as changed because our servers previously geocoded the location.
| Website URL | Must be a bare URL, such as http://www.google.com, or in the format 'Google (http://www.google.com)'.
| Email | Must be in the format foo@foo.com.
| Checkbox | Either "true" or "false". For data stored in the old backend, missing values are treated as false.
| Flag | Must be "Red", "Blue", "Green", "Yellow", "Orange" or "Purple".
| Star | Must be an integer from 1 to 5.
| Phone | Fully formed, this is a colon-separated pair of the phone number and phone type may be given. The phone number has no restrictions. The phone type, if provided, must be one of "Home", "Cell", "Work", "Fax", or "Other".  If only one or the other is provided, no colon is required. Examples are "(123) 456-7890", "555-1212:Home" and "Work".
| Multiple Choice| Not supported
| Photo (Image) | Not supported
| Document | Not supported
| Nested Table| Not supported
| Dataset Link| Not supported

