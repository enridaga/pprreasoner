#!/bin/bash

java -jar -Dlog4j.configuration="file:./experiment-log4j.properties" target/experiment-0.0.1-SNAPSHOT.jar "$@"

