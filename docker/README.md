# phoss SMP Docker configuration

This folder contains the Docker configuration file for phoss SMP.
It is based on the official `tomcat:8.5` image.

## Building

To build the SMP image use the following command:
```
docker build -t phoss-smp .
```

It exposes port 8080 where Tomcat is running.

### Version change
To change the version build you can specify the version on the commandline:

```
docker build --build-arg VERSION=5.0.1 -t phoss-smp-5.0.1 .
```

Note: since the file system layout changed between 5.0.0 and 5.0.1, the current version is only applicable to versions &ge; 5.0.1

## Running

Once the image is build you can run it with the following command.
```
docker run -d --name phoss-smp -p 8888:8080 phoss-smp
```

Upon successful completion opening `http://localhost:8888` in your browser should show you the start page of phoss SMP.
 
