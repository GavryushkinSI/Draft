#docker build -t parsesignal/draft:01.000.000 .
#docker push parsesignal/draft:01.000.000

#touch docker-compose.yml
#sudo nano docker-compose
#docker login -u parsesignal -p 45for896+
version: '3'
services:
  draft:
    image: parsesignal/draft:01.000.000
    container_name: draft
    ports:
      - 8080:8080

docker login -u parsesignal -p aspeka25y
docker-compose up

version: '3'
services:
  draft-back:
    image: "parsesignal/draft:back_01.000.000"
    container_name: draft-back
    ports:
      - 9000:9000

  draft-ui:
    image: "parsesignal/draft:ui_01.000.000"
    container_name: draft-ui
    ports:
      - 80:80
    links:
      - draft-back

find . -name '*.tsx' -delete