# This script builds the Docker containers and then runs them in a Docker composition

# Change to the folder of this script
$scriptpath = $MyInvocation.MyCommand.Path
$dir = Split-Path $scriptpath
Set-Location $dir

New-Item -Force temp -ItemType "directory"

Copy-Item ../client/target//mutualtls-client-SNAPSHOT.jar temp
Copy-Item ../server/target//mutualtls-server-SNAPSHOT.jar temp
Copy-Item ../keymat/client-keystore.p12 temp
Copy-Item ../keymat/client-truststore.p12 temp
Copy-Item ../keymat/server-keystore.p12 temp
Copy-Item ../keymat/server-truststore.p12 temp

# Remove-Item -Force -Recurse temp

docker build -t fujitsuuk/mutualtlstestclient:snapshot -f Dockerfile-client .
docker build -t fujitsuuk/mutualtlstestserver:snapshot -f Dockerfile-server .

docker compose up


