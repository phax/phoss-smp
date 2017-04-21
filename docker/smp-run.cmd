@echo off
docker run -d --name phoss-smp -p 8888:8080 phoss-smp
echo Open http://localhost:8888" in your browser
