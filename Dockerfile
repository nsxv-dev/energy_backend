FROM maven:3.9.5-openjdk-21 AS build

WORKDIR /app

COPY ./demo/pom.xml .
RUN mvn dependency:go-offline -B

COPY ./demo/src ./src

RUN mvn clean package -DskipTests

FROM openjdk-21

WORKDIR /app

COPY --from=build /app/target/*.jar ./app.jar

EXPOSE 8080


ENTRYPOINT ["java", "-jar", "app.jar"]