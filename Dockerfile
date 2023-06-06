FROM openjdk:17-slim

ADD target/nexus-provisioner-jar-with-dependencies.jar /provisioner.jar

ENTRYPOINT [ "/usr/local/openjdk-17/bin/java" ]
CMD [  "-jar" ,  "/provisioner.jar"]
