# meta1
cd meta1/
mvn compile

## correr gateway:
java -cp target/classes googol.gateway.Gateway

## correr barrel1:
java -cp target/classes googol.barrel.Barrel
# meta1
cd meta1/
mvn compile

## correr gateway:
java -cp target/classes googol.gateway.Gateway

## correr barrel1:
java -cp target/classes googol.barrel.Barrel

## correr barrel2:
docker-compose -f maquina2.yaml down
docker-compose -f maquina2.yaml build
docker-compose -f maquina2.yaml up barrel2

## correr downloader:
mvn exec:java '-Dexec.mainClass=googol.downloader.Downloader'

# frontend
mvn compile
mvn spring-boot:run