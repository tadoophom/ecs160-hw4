#!/bin/bash

cd spring-boot-app
mvn clean install
mvn spring-boot:run &
SPRING_PID=$!
cd ..

sleep 10

cd main-app
mvn clean install
mvn exec:java
cd ..

kill $SPRING_PID