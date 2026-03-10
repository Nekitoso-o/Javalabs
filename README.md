# NeMangalib
1. Управление каталогом
Добавление/редактирование/удаление произведений (манга, комиксы, ранобэ)
Добавление авторов и художников
Управление жанрами и тегами

2. Поиск и фильтрация
Поиск по названию, автору, жанру
Фильтры по статусу выпуска (выходит/завершён/анонс), году, рейтингу

3. Пользовательские коллекции
Добавление в списки: "Прочитано", "В процессе", "Буду читать", "Любимое"
Оценка произведения (по 10-балльной шкале)
Отзывы и комментарии

4. Детальная информация
Просмотр информации о произведении: описание, список глав/томов, авторы, жанры
Статистика: средняя оценка, количество прочитавших

---

Основные сущности (Entity):

1. User (Пользователь)
id, username, email, password_hash, registration_date
Связи: коллекции, оценки, комментарии

2. Title (Произведение)
id, title (название), original_title (оригинальное), description, type (манга/комикс/ранобэ), status (ONGOING/FINISHED/ANNOUNCED), release_year, cover_image_url
Связи: автор (Many-to-One), жанры (Many-to-Many), главы (One-to-Many)

3. Author (Автор/Художник)
id, name, birth_date, biography, photo_url
Связи: произведения (One-to-Many)

4. Genre (Жанр)
id, name, description
Связи: произведения (Many-to-Many)

5. Chapter/Volume (Глава/Том)
id, title, number (номер главы/тома), release_date, pages
Связи: произведение (Many-to-One)

6. UserCollection (Коллекция пользователя)
id, user_id, title_id, status (READ/READING/PLAN_TO_READ/FAVORITE), progress (сколько глав прочитано), rating (оценка)
· Связи: пользователь (Many-to-One), произведение (Many-to-One)

7. Review (Отзыв)
id, user_id, title_id, content (текст отзыва), rating, created_date
Связи: пользователь (Many-to-One), произведение (Many-to-One)
