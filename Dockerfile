FROM eclipse-temurin:17-jdk-alpine

WORKDIR ./demo/app

COPY ./demo/pom.xml .
RUN .mvnw dependency:go-offline -B

COPY ./demo/src ./src

RUN .mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "demo-0.0.1-SNAPSHOT.jar"]
