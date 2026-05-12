import axios from 'axios';

const BASE_URL = 'https://mangacatalog-backend.onrender.com/api';

const client = axios.create({
    baseURL: BASE_URL,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
});

client.interceptors.response.use(
    (response) => response,
    (error) => {
        // Достаём строку из разных мест
        let message = 'Неизвестная ошибка';

        if (error.response?.data) {
            const data = error.response.data;
            if (typeof data === 'string') {
                message = data;
            } else if (typeof data.message === 'string') {
                message = data.message;
            } else if (typeof data.error === 'string') {
                message = data.error;
            } else {
                message = JSON.stringify(data);
            }
        } else if (typeof error.message === 'string') {
            message = error.message;
        }

        return Promise.reject(new Error(message));
    }
);

// Comics
export const comicsApi = {
    getAll: () => client.get('/comics').then((r) => r.data),
    getById: (id) => client.get(`/comics/${id}`).then((r) => r.data),
    search: (title) => client.get('/comics/search', { params: { title } }).then((r) => r.data),
    getByAuthor: (authorId) => client.get(`/comics/author/${authorId}`).then((r) => r.data),
    complexSearch: (params) =>
        client.get('/comics/complex-search', { params }).then((r) => r.data),
    create: (data) => client.post('/comics', data).then((r) => r.data),
    update: (id, data) => client.put(`/comics/${id}`, data).then((r) => r.data),
    delete: (id) => client.delete(`/comics/${id}`).then((r) => r.data),
    patch: (id, data) => client.patch(`/comics/${id}`, data).then((r) => r.data),
};

// Authors
export const authorsApi = {
    getAll: () => client.get('/authors').then((r) => r.data),
    getById: (id) => client.get(`/authors/${id}`).then((r) => r.data),
    create: (data) => client.post('/authors', data).then((r) => r.data),
    update: (id, data) => client.put(`/authors/${id}`, data).then((r) => r.data),
    delete: (id) => client.delete(`/authors/${id}`).then((r) => r.data),
};

// Publishers
export const publishersApi = {
    getAll: () => client.get('/publishers').then((r) => r.data),
    getById: (id) => client.get(`/publishers/${id}`).then((r) => r.data),
    create: (data) => client.post('/publishers', data).then((r) => r.data),
    update: (id, data) => client.put(`/publishers/${id}`, data).then((r) => r.data),
    delete: (id) => client.delete(`/publishers/${id}`).then((r) => r.data),
};

// Genres
export const genresApi = {
    getAll: () => client.get('/genres').then((r) => r.data),
    getById: (id) => client.get(`/genres/${id}`).then((r) => r.data),
    create: (data) => client.post('/genres', data).then((r) => r.data),
    update: (id, data) => client.put(`/genres/${id}`, data).then((r) => r.data),
    delete: (id) => client.delete(`/genres/${id}`).then((r) => r.data),
};

// Reviews
export const reviewsApi = {
    getAll: () => client.get('/reviews').then((r) => r.data),
    getById: (id) => client.get(`/reviews/${id}`).then((r) => r.data),
    getByComicId: (comicId) =>
        client.get(`/reviews/comic/${comicId}`).then((r) => r.data),
    create: (data) => client.post('/reviews', data).then((r) => r.data),
    update: (id, data) => client.put(`/reviews/${id}`, data).then((r) => r.data),
    delete: (id) => client.delete(`/reviews/${id}`).then((r) => r.data),
    patch: (id, data) => client.patch(`/reviews/${id}`, data).then((r) => r.data),
};

// Comic Images
export const comicImagesApi = {
    getAll: (comicId) =>
        client.get(`/comics/${comicId}/images`).then((r) => r.data),

    upload: (comicId, files) => {
        const form = new FormData();
        Array.from(files).forEach((f) => form.append('files', f));
        return client.post(`/comics/${comicId}/images`, form, {
            headers: { 'Content-Type': 'multipart/form-data' },
        }).then((r) => r.data);
    },

    delete: (comicId, imageId) =>
        client.delete(`/comics/${comicId}/images/${imageId}`).then((r) => r.data),

    reorder: (comicId, orderedIds) =>
        client.put(`/comics/${comicId}/images/reorder`, orderedIds).then((r) => r.data),
};

// Comic Chapters
export const comicChaptersApi = {
    getAll: (comicId) =>
        client.get(`/comics/${comicId}/chapters`).then((r) => r.data),

    getOne: (comicId, chapterId) =>
        client.get(`/comics/${comicId}/chapters/${chapterId}`).then((r) => r.data),

    create: (comicId, chapterNumber, title, files) => {
        const form = new FormData();
        form.append('chapterNumber', chapterNumber);
        if (title) form.append('title', title);
        Array.from(files).forEach((f) => form.append('files', f));
        return client.post(`/comics/${comicId}/chapters`, form, {
            headers: { 'Content-Type': 'multipart/form-data' },
            timeout: 120000,
        }).then((r) => r.data);
    },

    delete: (comicId, chapterId) =>
        client.delete(`/comics/${comicId}/chapters/${chapterId}`).then((r) => r.data),
};