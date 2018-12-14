run:
	java -jar target/DVBatch-1.0-SNAPSHOT-jar-with-dependencies.jar token=$$API_TOKEN secret=$$API_SECRET

install:
	mvn compile assembly:single

clean:
	mvn clean
