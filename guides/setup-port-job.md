

Port jobs are used for moving data around that is already on the Socrata platform. Users that have publisher rights can make copies of existing datasets using this this tool. Port jobs allow the copying of both dataset schemas (metadata and columns) and data (rows).

### Configuring in GUI mode

In the DataSync UI go to `File -> New... -> Port Job`. Use the help bubbles (marked with blue '?') in the UI to guide you through setting up a Port Job.

More detailed documentation comming soon. 

### Configuring in Command-line/Headless Mode

To duplicate a dataset with a PortJob run the following command (filling in the parameters):

```
java -jar datasync.jar -c <CONFIG FILE> -t PortJob -pm copy_all -pd1 <SOURCE DOMAIN> -pi1 <SOURCE DATASET ID> -pd2 <DESTINATION DOMAIN>  -pdt <TITLE OF NEW DATASET> -pp true
```

This will create a copy of the given source dataset on the destination domain titled `<TITLE OF NEW DATASET>`. The newly created dataset will always be created as Private (and can be changed to public manually using the dataset UI). 

If you have not already established global configuration then you must include this flag: 
`-c <CONFIG FILE>`
To use configuration in DataSync “memory”, simply omit the `-c <CONFIG FILE>` flag.

If you want to copy only the dataset schema (without copying the data) use:
`-m copy_schema`

If you do not want the resulting dataset to be published (and stay as a working copy) use this flag:
`-pp false`

Example of complete PortJob command:

```
java -jar datasync.jar -c config.json -t PortJob -pm copy_schema -pd1 https://data.cityofchicago.org -pi1 97wa-y6ff -pd2 https://data.cityofchicago.org -pdt ‘Port Job Test Title’ -pp true
```
