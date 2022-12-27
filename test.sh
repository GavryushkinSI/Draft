docker build -t parsesignal/draft:01.000.000 .
docker push parsesignal/draft:01.000.000

touch docker-compose.yml
sudo nano docker-compose
version: '3'
services:
  draft:
    image: parsesignal/draft:01.000.000
    container_name: draft
    ports:
      - 8080:8080

docker login -u parsesignal -p aspeka25y
