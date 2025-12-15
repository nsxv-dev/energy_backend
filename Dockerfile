FROM eclipse-temurin:17-jdk-alpine

WORKDIR ./demo/app

COPY ./demo/pom.xml .
RUN mvn dependency:go-offline -B

COPY ./demo/src ./src

RUN mvn clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "demo-0.0.1-SNAPSHOT.jar"]
