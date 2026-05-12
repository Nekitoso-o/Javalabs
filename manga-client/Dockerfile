# ─── Этап 1: Сборка React ─────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG VITE_API_URL=""
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build

# ─── Этап 2: Nginx ────────────────────────────────────────────────
FROM nginx:stable-alpine

# Аргумент — URL бэкенда, подставляется при сборке на Render
ARG BACKEND_URL=http://localhost:8080

RUN rm /etc/nginx/conf.d/default.conf

COPY nginx.conf /etc/nginx/conf.d/app.conf

# Подставляем реальный URL бэкенда прямо в конфиг во время сборки образа
RUN sed -i "s|BACKEND_URL_PLACEHOLDER|${BACKEND_URL}|g" \
    /etc/nginx/conf.d/app.conf

COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]