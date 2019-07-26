# DV-Batch-Java

Command-line client that can be used by prospect merchants to test Netverify service.

## Getting Started

### Install Java JDK

Java JDK is needed to compile the program. Please download [here](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

Please make sure the JAVA_HOME environment variable is set to JDK.

### Install Maven

Maven can be downloaded and installed directly [here](http://maven.apache.org/download.html).

On PC, please follow instruction [here](https://maven.apache.org/install.html).

On Mac, Maven can also be installed through Homebrew. Please check [here](https://brew.sh/) for Homebrew installation.

```
$ brew install maven
```

### Clean the project directory

Use below command to make sure the environment is clean. Open the command prompt or terminal and enter below.

```
$ mvn clean
```

### Build the program

At the project's root directory, use Maven to compile the program.

```
$ mvn compile assembly:single
```

After compilation, 'DVBatch-1.0-SNAPSHOT-jar-with-dependencies.jar' should appear in the 'target' directory. This file is the compiled Java program.

## Running the program

### Edit config.properties

Below are the parameters that can be customized. When parameter value is specified both in config file and the command line, the command line value takes precedence.

Name|Command Line Arg|Example
---|---|---
pathToImageFolder |yes	|docs
serverUrl	|yes	|https://acquisition.netverify.com/api/netverify/v2/acquisitions
merchantReportingCriteria	|no	|Remediation
numberToSubmit|yes |2

### Set API token/secret in environment variables (optional)

Netverify requires authentication through API token and secret. They can be stored in environment variables for easy access.

```
$ export API_TOKEN=********
$ export API_SECRET=********
```

### Execution

1. Name the doc file with customerId. For example, '<customer_id>.pdf'.

2. Take 'DVBatch-1.0-SNAPSHOT-jar-with-dependencies.jar' and copy to the image folder (optional).

3. Use below command to run the program.

```
$ java -jar target/DVBatch-1.0-SNAPSHOT-jar-with-dependencies.jar token=$API_TOKEN secret=$API_SECRET 
```

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management
