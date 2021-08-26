FROM openjdk:11-jdk
EXPOSE 3333:3333
RUN mkdir /app
COPY ./build/install/fan-monitor/ /app/
WORKDIR /app/bin
CMD ["./fan-monitor"]
