install:
	mvn compile assembly:single

run:
	java -jar target/DVBatch-1.0-SNAPSHOT-jar-with-dependencies.jar token=$$API_TOKEN secret=$$API_SECRET

clean:
	mvn clean
