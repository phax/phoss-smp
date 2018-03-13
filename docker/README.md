# phoss SMP Docker configuration

This folder contains the Docker configuration file for phoss SMP.
It is based on the official `tomcat:8.5` image.

Prebuild images are available from https://hub.docker.com/r/phelger/smp/

**Note:** The SMP comes pretty unconfigured

Note: the `Dockerfile-release-binary-xml` builds the latest release with the XML backend.

Note: the `Dockerfile-release-binary-sql` builds the latest release with the SQL backend

Note: the `Dockerfile-release-from-source-xml` build the latest release from GitHub with XML backend

Note: the `Dockerfile-snapshot-from-source-xml` build the latest snapshot from GitHub with XML backend

## Release Binary, XML Backend

To build, run and stop the SMP image with XML backend use the following command:

```
docker build -t phoss-smp-release-binary-xml -f Dockerfile-release-binary-xml .
docker run -d --name phoss-smp-release-binary-xml -p 8888:8080 phoss-smp-release-binary-xml
docker stop phoss-smp-release-binary-xml
docker rm phoss-smp-release-binary-xml
```

It exposes port 8888 where Tomcat is running successfully.
Open `http://localhost:8888` in your browser.

## Release Binary, SQL backend

To build the SMP image with XML backend use the following command:

```
docker build -t phoss-smp-release-binary-sql -f Dockerfile-release-binary-sql .
docker run -d --name phoss-smp-release-binary-sql -p 8888:8080 phoss-smp-release-binary-sql
docker stop phoss-smp-release-binary-sql
docker rm phoss-smp-release-binary-sql
```

It exposes port 8888 where Tomcat is running successfully.
Open `http://localhost:8888` in your browser.

## Release from source, XML Backend

```
docker build -t phoss-smp-release-from-source-xml -f Dockerfile-release-from-source-xml .
docker run -d --name phoss-smp-release-from-source-xml -p 8888:8080 phoss-smp-release-from-source-xml
docker stop phoss-smp-release-from-source-xml
docker rm phoss-smp-release-from-source-xml
```

It exposes port 8888 where Tomcat is running successfully.
Open `http://localhost:8888` in your browser.

## Latest version from source, XML Backend

```
docker build -t phoss-smp-snapshot-from-source-xml -f Dockerfile-snapshot-from-source-xml .
docker run -d --name phoss-smp-snapshot-from-source-xml -p 8888:8080 phoss-smp-snapshot-from-source-xml
docker stop phoss-smp-snapshot-from-source-xml
docker rm phoss-smp-snapshot-from-source-xml
```

It exposes port 8888 where Tomcat is running successfully.
Open `http://localhost:8888` in your browser.

# Misc Docker related stuff

## Version change
To change the version build of release versions you can specify the version on the commandline when building:

```
docker build --build-arg VERSION=5.0.3 -t phoss-smp-5.0.3 .
```

Note: since the file system layout changed between 5.0.0 and 5.0.1, the current version is only applicable to versions &ge; 5.0.1

## Running pre-build image from Docker Hub

Running a pre-build image (XML backend only):
```
docker run -d --name phoss-smp-release-binary-xml-5.0.3 -p 8888:8080 phelger/smp:5.0.3
docker stop phoss-smp-release-binary-xml-5.0.3
docker rm phoss-smp-release-binary-xml-5.0.3
```

It exposes port 8888 where Tomcat is running successfully.
Open `http://localhost:8888` in your browser.

## Docker cheatsheet

Short explanation on docker running
  * `-d` - run in daemon mode
  * `--name phoss-smp` - internal nice name for `docker ps` etc.
  * `-p 8888:8080` - proxy container port 8080 to host port 8888
  * `phoss-smp` - the tag to be run

Upon successful completion opening http://localhost:8888 in your browser should show you the start page of phoss SMP.

Default credentials are in the Wiki at https://github.com/phax/peppol-smp-server/wiki/Running#default-login

The data directory inside the Docker image, where the data is stored is usually `/home/git/conf`.
 
To check the log file use `docker logs phoss-smp`. There is no catalina.out file - only a catalina.out.yyyy-mm-dd.

To open a shell in the docker image use `docker exec -it phoss-smp-snapshot-from-source-xml bash` where `phoss-smp-snapshot-from-source-xml` is the name of the machine.
 
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
