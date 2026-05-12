# ─── Этап 1: Сборка React-приложения ─────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# Копируем package.json и lock-файл отдельно для кэширования зависимостей
COPY package.json package-lock.json ./

# Устанавливаем зависимости (ci — строго по lock-файлу, быстрее)
RUN npm ci

# Копируем исходники
COPY . .

# Передаём URL бэкенда через build-аргумент
# При сборке на Render он будет подставлен автоматически
ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=$VITE_API_URL

# Собираем продакшн-бандл
RUN npm run build

# ─── Этап 2: Nginx для раздачи статики ───────────────────────────
FROM nginx:stable-alpine

# Удаляем дефолтный конфиг nginx
RUN rm /etc/nginx/conf.d/default.conf

# Копируем наш конфиг
COPY nginx.conf /etc/nginx/conf.d/app.conf

# Копируем собранный бандл из этапа 1
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]