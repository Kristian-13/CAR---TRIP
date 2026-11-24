# Návod pro hosting (např. Render.com), jak aplikaci sestavit a spustit.
FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY src ./src
COPY public ./public
RUN javac -encoding UTF-8 -d bin $(find src -name "*.java")
EXPOSE 8080
CMD ["java", "-cp", "bin", "WebApp"]
