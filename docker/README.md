# phoss SMP Docker configuration

This folder contains the Docker configuration files for phoss SMP.
It is based on the official `tomcat:10.1-jdk17` image since v7.0.0.
It is recommended to run production Docker images with at least 4GB of RAM.

Prebuild images are available from:
* https://hub.docker.com/r/phelger/

**Note:** the `smp` directory contains current build SNAPSHOTs as well as old XML binary releases. For the most up-to-date versions use `phoss-smp-(xml|sql|mongodb)` folders:
* https://hub.docker.com/r/phelger/phoss-smp-mongodb
* https://hub.docker.com/r/phelger/phoss-smp-sql
* https://hub.docker.com/r/phelger/phoss-smp-xml

**Note:** The SMP comes pretty unconfigured

**Note:** The default exposed port of all of these images is 8080.

**Note:** The Tomcat logs inside the image are residing in `/usr/local/tomcat/logs`

# Running pre-build image from Docker Hub

Note: running the pre-build image assumes that you adopted the [configuration](https://github.com/phax/phoss-smp/wiki/Configuration) of the SMP so that it suits your needs.

Running a pre-build image (XML backend only):

```
docker run -d --name phoss-smp -p 8888:8080 phelger/phoss-smp-xml:latest
docker stop phoss-smp
docker rm phoss-smp
```

Alternative console without any configuration - for quick startup testing only:
```
docker run --name phoss-smp -p 8888:8080 -v.:/a -e WEBAPP_DATAPATH=/a -e SML_SMPID=TEST1 -e SMP_BACKEND=xml phelger/phoss-smp-xml:latest
```
and for ARM64 architecture:
```
docker run --name phoss-smp -p 8888:8080 -v.:/a -e WEBAPP_DATAPATH=/a -e SML_SMPID=TEST1 -e SMP_BACKEND=xml phelger/phoss-smp-xml-arm64:latest
```

The command line exposes port 8888 locally if Tomcat is running successfully.
Open `http://localhost:8888` in your browser to check.

## SMP file storage

### Referencing configuration files

My suggestion is to create a child dockerfile like `Example-Dockerfile-with-configuration`.
It sets the system properties to the SMP configuration files in the virtual path `/config`.
See https://docs.docker.com/storage/volumes/ for the Docker configuration on volumes and mount points.

The main change is the `-v` parameter that mounts a local directory into the running image. `/host-directory/config` in the example down below must be changed to an existing directory containing the files `application.properties` (since v5.3.x) (as named in the example dockerfile).

```
docker build --pull -t phoss-smp-with-config -f Example-Dockerfile-with-configuration .
docker run -d --name phoss-smp-with-config -p 8888:8080 -v /host-directory/config:/config phoss-smp-with-config
docker stop phoss-smp-with-config
docker rm phoss-smp-with-config
```

Note: if you change `/config` as the image directory to something else, please ensure to change the paths in the `example-config-dir/` properties files as well. 

### Persistent data storage for XML backend 

To persistently save all the data stored by the SMP add another volume that mounts the docker directory `/home/git/conf` to a local directory as in `-v /host-directory/data:/home/git/conf`.

### All setup together

On my Windows machine I use the following command to run the whole SMP on port 8888 with the correct configuration and the persistent storage like this: 

```
docker run -d --name phoss-smp-with-config -p 8888:8080 -v c:\dev\git\phoss-smp\docker\example-config-dir:/config -v C:\dev\git\phoss-smp\docker\persistent\:/home/git/conf phoss-smp-with-config
```

### Usage of environment variables

Alternatively the full paths of the configuration properties can also be provided with environment variables directly on the `docker run` commandline:

#### For v6.x and onwards

```
-e "CONFIG_FILE=/config/application.properties"
```

as in

```shell
docker run -d --name phoss-smp -p 80:8080 \
 -e "CONFIG_FILE=/config/application.properties" \
 -v /home/ubuntu/config:/config \
 phelger/phoss-smp-xml:latest
```

# Building Docker images

This section is purely a reminder to myself on how to do it.

Note: the `Dockerfile-release-binary-xml` builds the latest release from binaries with XML backend.

Note: the `Dockerfile-release-binary-sql` builds the latest release from binaries with SQL backend

Note: the `Dockerfile-release-binary-mongodb` builds the latest release from binaries with MongoDB backend (since v5.2.0)

Note: the `Dockerfile-release-from-source-xml` build the latest release from GitHub sources with XML backend

Note: the `Dockerfile-snapshot-from-source-xml` build the latest snapshot from GitHub sources with XML backend

Note: the `Dockerfile-snapshot-from-source-sql` build the latest snapshot from GitHub sources with SQL backend

Note: the `Dockerfile-snapshot-from-source-mongodb` build the latest snapshot from GitHub sources with MongoDB backend

### Release Binary, XML Backend

Use an existing binary release, with the XML backend.

To build, run and stop the SMP image with XML backend use the following command:

```
docker build --pull -t phoss-smp-release-binary-xml -f Dockerfile-release-binary-xml --build-arg SMP_VERSION=7.2.6 .
docker run -d --name phoss-smp-release-binary-xml -p 8888:8080 phoss-smp-release-binary-xml
docker stop phoss-smp-release-binary-xml
docker rm phoss-smp-release-binary-xml
```

It exposes port 8888 where Tomcat is running successfully.
Open `http://localhost:8888` in your browser.

### Release Binary, SQL backend

Use an existing binary release, with the SQL backend.

To build the SMP image with SQL backend use the following command:

```
docker build --pull -t phoss-smp-release-binary-sql -f Dockerfile-release-binary-sql --build-arg SMP_VERSION=7.2.6 .
docker run -d --name phoss-smp-release-binary-sql -p 8888:8080 phoss-smp-release-binary-sql
docker stop phoss-smp-release-binary-sql
docker rm phoss-smp-release-binary-sql
```

It exposes port 8888 where Tomcat is running successfully.
Open `http://localhost:8888` in your browser.


### Release Binary, MongoDB backend

Use an existing binary release, with the MongoDB backend.

To build the SMP image with MongoDB backend use the following command:

```
docker build --pull -t phoss-smp-release-binary-mongodb -f Dockerfile-release-binary-mongodb --build-arg SMP_VERSION=7.2.6 .
docker run -d --name phoss-smp-release-binary-mongodb -p 8888:8080 phoss-smp-release-binary-mongodb
docker stop phoss-smp-release-binary-mongodb
docker rm phoss-smp-release-binary-mongodb
```

It exposes port 8888 where Tomcat is running successfully.
Open `http://localhost:8888` in your browser.

### Release from source, XML Backend

Build the SMP from GitHub source with the XML backend using the tag of the last release.

```
docker build --pull -t phoss-smp-release-from-source-xml -f Dockerfile-release-from-source-xml --build-arg SMP_VERSION=7.2.6 .
docker run -d --name phoss-smp-release-from-source-xml -p 8888:8080 phoss-smp-release-from-source-xml
docker stop phoss-smp-release-from-source-xml
docker rm phoss-smp-release-from-source-xml
```

It exposes port 8888 where Tomcat is running successfully.
Open `http://localhost:8888` in your browser.

### Latest snapshot version from source, MongoDB backend

Build the SMP from GitHub source with the MongoDB backend using the HEAD version of the master branch (SNAPSHOT version).

```
docker build --pull -t phoss-smp-snapshot-from-source-mongodb -f Dockerfile-snapshot-from-source-mongodb .
docker run -d --name phoss-smp-snapshot-from-source-mongodb -p 8888:8080 phoss-smp-snapshot-from-source-mongodb
docker stop phoss-smp-snapshot-from-source-mongodb
docker rm phoss-smp-snapshot-from-source-mongodb
```

### Latest snapshot version from source, SQL backend

Build the SMP from GitHub source with the SQL backend using the HEAD version of the master branch (SNAPSHOT version).

```
docker build --pull -t phoss-smp-snapshot-from-source-sql -f Dockerfile-snapshot-from-source-sql .
docker run -d --name phoss-smp-snapshot-from-source-sql -p 8888:8080 phoss-smp-snapshot-from-source-sql
docker stop phoss-smp-snapshot-from-source-sql
docker rm phoss-smp-snapshot-from-source-sql
```

### Latest snapshot version from source, XML Backend

Build the SMP from GitHub source with the XML backend using the HEAD version of the master branch (SNAPSHOT version).

```
docker build --pull -t phoss-smp-snapshot-from-source-xml -f Dockerfile-snapshot-from-source-xml .
docker run -d --name phoss-smp-snapshot-from-source-xml -p 8888:8080 phoss-smp-snapshot-from-source-xml
docker stop phoss-smp-snapshot-from-source-xml
docker rm phoss-smp-snapshot-from-source-xml
```

It exposes port 8888 where Tomcat is running successfully.
Open `http://localhost:8888` in your browser.

### Version change

To change the version build of binary release versions you can specify the version on the commandline when building:

```
docker build --build-arg SMP_VERSION=7.2.6 -t phoss-smp-release-binary-xml-7.2.6 -f Dockerfile-release-binary-xml .
```

### Docker cheatsheet

Short explanation on docker running
  * `-d` - run in daemon mode
  * `--name phoss-smp` - internal nice name for `docker ps`, `docker logs` etc.
  * `-p 8888:8080` - proxy container port 8080 to host port 8888
  * `phoss-smp` - the tag to be run

Upon successful completion opening http://localhost:8888 in your browser should show you the start page of phoss SMP.

Default credentials are in the Wiki at https://github.com/phax/phoss-smp/wiki/Running#default-login

The data directory inside the Docker image where the data is usually stored is `/home/git/conf`.
 
To check the log file use `docker logs phoss-smp`. There is no `catalina.out` file - only a `catalina.out.yyyy-mm-dd`.

To open a shell in the docker image use `docker exec -it phoss-smp-snapshot-from-source-xml bash` where `phoss-smp-snapshot-from-source-xml` is the name of the machine.
 
### Pushing changes

Once a new version is available the image needs to be build and pushed to Docker hub:

```
docker login
docker tag phoss-smp-release-binary-xml-x.y.z phelger/smp:x.y.z
docker push phelger/smp:x.y.z
docker tag phoss-smp-release-binary-xml-x.y.z phelger/smp:latest
docker push phelger/smp:latest
docker logout
```

See file `build-release-latest.cmd` for the effective build script.
