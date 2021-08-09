# MutualTLSIssueReproducer
 Reproduces the issue of Mutual TLS failing from Java 11.0.12 onwards (works in Java 11.0.11)
 
## Instructions
1. Create keymat (see Powershell script in _keymat_ folder)
2. Build the Java components (pom.xml) in root folder using _mvn_ _clean_ _install_
3. Build and run Dockerised version of client and server (see _run.ps1_ Powershell script in _docker_ folder)

## Things to watch for
When _docker/Dockerfile-client_ and _docker/Dockerfile-server_ are both based on 11.0.12 version of OpenJDK, the HTTP GET fails with bad certificate error.
If you swap 11.0.12 in both of the above files for 11.0.11, it works fine.
Have tried both the vanilla openjdk/11.0.12 and amazoncorretto/11.0.12 as a base image; same result each time so not the distro.
