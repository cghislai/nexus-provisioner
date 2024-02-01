FROM openjdk:21-slim

ADD target/nexus-provisioner-jar-with-dependencies.jar /provisioner.jar

ENTRYPOINT [ "/usr/local/openjdk-15/bin/java" ]
CMD [  "-jar" ,  "/provisioner.jar"]
