FROM node:22.23.2-alpine3.24 AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY public ./public
COPY src ./src

ARG REACT_APP_SERVER=http://localhost:8080
ENV REACT_APP_SERVER=${REACT_APP_SERVER}
ENV PUBLIC_URL="" REACT_APP_GITHUB_PAGES=false CI=true

RUN npm run build

FROM nginx:1.30.4-alpine3.24

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/build /usr/share/nginx/html
ARG RELEASE_ID=development
RUN printf '{"release":"%s"}\n' "$RELEASE_ID" > /usr/share/nginx/html/release.json
LABEL org.opencontainers.image.revision=$RELEASE_ID

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
