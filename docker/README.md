# phoss SMP Docker configuration

This folder contains the Docker configuration file for phoss SMP.
It is based on the official `tomcat:8.5` image.

Prebuild images are available from https://hub.docker.com/r/phelger/smp/

**Note:** The SMP comes pretty unconfigured - this is work in progress (2017-04-21)

## Building

To build the SMP image use the following command:
```
docker build -t phoss-smp-latest .
```

It exposes port 8080 where Tomcat is running.

### Version change
To change the version build you can specify the version on the commandline:

```
docker build --build-arg VERSION=5.0.1 -t phoss-smp-5.0.1 .
```

Note: since the file system layout changed between 5.0.0 and 5.0.1, the current version is only applicable to versions &ge; 5.0.1

## Running

Running a pre-build image:
```
docker run -d --name phoss-smp -p 8888:8080 phelger/smp:latest
```

Once the image is build you can run it with the following command.
```
docker run -d --name phoss-smp -p 8888:8080 phoss-smp
```

Short explanation
  * `-d` - run in daemon mode
  * `--name phoss-smp` - internal nice name for `docker ps` etc.
  * `-p 8888:8080` - proxy container port 8080 to host port 8888
  * `phoss-smp-latest` - the tag to be run

Upon successful completion opening http://localhost:8888 in your browser should show you the start page of phoss SMP.
 
## Stopping

When you don't need the image anymore stop and remove it like this:
```
docker stop phoss-smp
docker rm phoss-smp
```

## Pushing changes

Once a new version is available the image needs to be build and pushed to Docker hub:
```
docker login
docker tag phoss-smp-x.y.z phelger/smp:x.y.z
docker push phelger/smp:x.y.z
docker tag phoss-smp-x.y.z phelger/smp:latest
docker push phelger/smp:latest
docker logout
```
