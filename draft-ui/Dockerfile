### STAGE 1: Build ###
FROM node:16.13.0-alpine AS build
WORKDIR /usr/src/app
COPY package.json ./
RUN npm install
COPY . .
RUN npm run build

### STAGE 2: Run ###
FROM nginx:1.17.1-alpine
COPY default.conf /etc/nginx/conf.d/default.conf
COPY src/index.html /usr/share/nginx/html/index.html
#COPY --from=public /usr/src/app/public /usr/share/nginx/html
COPY --from=build /usr/src/app/build /usr/share/nginx/html
EXPOSE 80