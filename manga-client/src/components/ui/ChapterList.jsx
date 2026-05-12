import { useState, useEffect, useCallback } from 'react';
import { comicChaptersApi } from '../../api/api.js';
import ChapterReader from './ChapterReader.jsx';

function AddChapterModal({ comicId, onDone, onCancel, existingNumbers }) {
    const [number, setNumber] = useState('');
    const [title, setTitle] = useState('');
    const [files, setFiles] = useState(null);
    const [dragging, setDragging] = useState(false);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleDrop = (e) => {
        e.preventDefault();
        setDragging(false);
        const arr = Array.from(e.dataTransfer.files).filter((f) =>
            f.type.startsWith('image/')
        );
        if (arr.length) setFiles(arr);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        const num = parseFloat(number);
        if (!number || isNaN(num) || num <= 0) {
            setError('Введите корректный номер главы');
            return;
        }
        if (existingNumbers.includes(num)) {
            setError(`Глава ${num} уже существует`);
            return;
        }
        if (!files || files.length === 0) {
            setError('Загрузите хотя бы одну страницу');
            return;
        }
        setLoading(true);
        try {
            await comicChaptersApi.create(comicId, num, title.trim(), files);
            onDone();
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="chapter-modal-overlay" onClick={onCancel}>
            <div
                className="chapter-modal"
                onClick={(e) => e.stopPropagation()}
            >
                <div className="chapter-modal__header">
                    <h3>📖 Добавить главу</h3>
                    <button className="modal__close" onClick={onCancel}>
                        ✕
                    </button>
                </div>
                <form onSubmit={handleSubmit} className="chapter-modal__body">
                    <div className="form-group">
                        <label className="form-label">
                            Номер главы <span className="required">*</span>
                        </label>
                        <input
                            className="form-input"
                            type="number"
                            min="0.1"
                            step="0.1"
                            placeholder="Например: 1 или 1.5"
                            value={number}
                            onChange={(e) => setNumber(e.target.value)}
                            autoFocus
                        />
                    </div>
                    <div className="form-group">
                        <label className="form-label">Название</label>
                        <input
                            className="form-input"
                            placeholder="Необязательно"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                        />
                    </div>
                    <div className="form-group">
                        <label className="form-label">
                            Страницы <span className="required">*</span>
                        </label>
                        <label
                            className={`chapter-dropzone${dragging ? ' dragging' : ''}${files ? ' has-files' : ''}`}
                            onDragOver={(e) => {
                                e.preventDefault();
                                setDragging(true);
                            }}
                            onDragLeave={() => setDragging(false)}
                            onDrop={handleDrop}
                        >
                            <input
                                type="file"
                                accept="image/*"
                                multiple
                                style={{ display: 'none' }}
                                onChange={(e) => {
                                    const arr = Array.from(e.target.files).filter(
                                        (f) => f.type.startsWith('image/')
                                    );
                                    if (arr.length) setFiles(arr);
                                    e.target.value = '';
                                }}
                            />
                            {files?.length > 0 ? (
                                <div className="chapter-dropzone__info">
                                    <span style={{ fontSize: 32 }}>✅</span>
                                    <span
                                        style={{
                                            fontWeight: 700,
                                            color: 'var(--success)',
                                        }}
                                    >
                                        Выбрано: {files.length} стр.
                                    </span>
                                    <span
                                        style={{
                                            fontSize: 12,
                                            color: 'var(--text-muted)',
                                        }}
                                    >
                                        Нажмите чтобы изменить
                                    </span>
                                </div>
                            ) : (
                                <div className="chapter-dropzone__info">
                                    <span
                                        style={{ fontSize: 40, opacity: 0.4 }}
                                    >
                                        🖼️
                                    </span>
                                    <span>Перетащите страницы сюда</span>
                                    <span
                                        style={{
                                            fontSize: 12,
                                            color: 'var(--text-muted)',
                                        }}
                                    >
                                        или нажмите для выбора
                                    </span>
                                    <span
                                        style={{
                                            fontSize: 11,
                                            color: 'var(--text-muted)',
                                        }}
                                    >
                                        JPG, PNG, WebP · до 20 MB каждый
                                    </span>
                                </div>
                            )}
                        </label>

                        {files?.length > 0 && (
                            <div className="chapter-files-preview">
                                {Array.from(files)
                                    .slice(0, 8)
                                    .map((f, i) => (
                                        <div
                                            key={i}
                                            className="chapter-files-preview__item"
                                        >
                                            <img
                                                src={URL.createObjectURL(f)}
                                                alt={f.name}
                                            />
                                            <span>{i + 1}</span>
                                        </div>
                                    ))}
                                {files.length > 8 && (
                                    <div className="chapter-files-preview__more">
                                        +{files.length - 8}
                                    </div>
                                )}
                            </div>
                        )}
                    </div>

                    {error && <div className="form-error">{error}</div>}
                    {loading && (
                        <div
                            style={{
                                color: 'var(--text-muted)',
                                fontSize: 13,
                                marginBottom: 8,
                            }}
                        >
                            ⏳ Загрузка страниц на сервер...
                        </div>
                    )}

                    <div
                        style={{
                            display: 'flex',
                            gap: 12,
                            justifyContent: 'flex-end',
                        }}
                    >
                        <button
                            type="button"
                            className="btn btn--ghost"
                            onClick={onCancel}
                            disabled={loading}
                        >
                            Отмена
                        </button>
                        <button
                            type="submit"
                            className="btn btn--primary"
                            disabled={loading}
                        >
                            {loading ? '⏳ Загрузка...' : '💾 Сохранить главу'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default function ChapterList({
                                        comicId,
                                        editMode = false,
                                        initialChapterId = null,
                                        onInitialChapterHandled = null,
                                    }) {
    const [chapters, setChapters] = useState([]);
    const [sortDesc, setSortDesc] = useState(true);
    const [showAdd, setShowAdd] = useState(false);
    const [readingChapter, setReadingChapter] = useState(null);
    const [loadingChapter, setLoadingChapter] = useState(null);
    const [alert, setAlert] = useState(null);
    const [confirmDelete, setConfirmDelete] = useState(null);

    const showAlert = (type, text) => {
        setAlert({ type, text });
        setTimeout(() => setAlert(null), 3000);
    };

    const load = useCallback(() => {
        comicChaptersApi
            .getAll(comicId)
            .then(setChapters)
            .catch(() => {});
    }, [comicId]);

    useEffect(() => {
        load();
    }, [load]);

    const handleRead = useCallback(
        async (chapter) => {
            setLoadingChapter(chapter.id);
            try {
                const full = await comicChaptersApi.getOne(
                    comicId,
                    chapter.id
                );
                setReadingChapter(full);
            } catch (e) {
                showAlert('error', `❌ ${e.message}`);
            } finally {
                setLoadingChapter(null);
            }
        },
        [comicId]
    );

    // Автооткрытие первой главы при нажатии кнопки "Читать"
    useEffect(() => {
        if (!initialChapterId || chapters.length === 0) return;

        const target = chapters.find((ch) => ch.id === initialChapterId);
        if (!target) return;

        handleRead(target);

        if (onInitialChapterHandled) onInitialChapterHandled();
    }, [initialChapterId, chapters, handleRead, onInitialChapterHandled]);

    const sorted = [...chapters].sort((a, b) =>
        sortDesc
            ? b.chapterNumber - a.chapterNumber
            : a.chapterNumber - b.chapterNumber
    );

    const askDelete = (e, chapter) => {
        e.stopPropagation();
        setConfirmDelete(chapter);
    };

    const confirmDeleteChapter = async () => {
        if (!confirmDelete) return;
        const chapter = confirmDelete;
        try {
            await comicChaptersApi.delete(comicId, chapter.id);
            load();
            showAlert(
                'success',
                `✅ Глава ${chapter.chapterNumber} удалена`
            );
        } catch (err) {
            showAlert('error', `❌ ${err.message}`);
        } finally {
            setConfirmDelete(null);
        }
    };

    const handleChapterChange = async (chapter) => {
        const full = await comicChaptersApi.getOne(comicId, chapter.id);
        setReadingChapter(full);
    };

    const existingNumbers = chapters.map((c) => c.chapterNumber);

    const formatDate = (iso) => {
        if (!iso) return '';
        return new Date(iso).toLocaleDateString('ru-RU', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
        });
    };

    return (
        <div className="ch-list">
            {alert && (
                <div
                    className={`alert alert--${alert.type}`}
                    style={{ marginBottom: 12 }}
                >
                    {alert.text}
                </div>
            )}

            {/* Тулбар */}
            <div className="ch-toolbar">
                <button
                    className="ch-toolbar__btn"
                    onClick={() => setSortDesc((v) => !v)}
                    title="Изменить порядок сортировки"
                >
                    <span className="ch-toolbar__btn-icon">⇅</span>
                    Сортировать
                    <span
                        style={{ fontSize: 11, color: 'var(--text-muted)' }}
                    >
                        {sortDesc
                            ? '(новые сначала)'
                            : '(старые сначала)'}
                    </span>
                </button>

                {editMode && (
                    <button
                        className="ch-toolbar__btn ch-toolbar__btn--accent"
                        onClick={() => setShowAdd(true)}
                    >
                        <span>➕</span> Добавить главу
                    </button>
                )}
            </div>

            <div className="ch-divider" />

            {/* Список */}
            {chapters.length === 0 ? (
                <div className="ch-empty">
                    <span style={{ fontSize: 48, opacity: 0.2 }}>📭</span>
                    <span>Глав пока нет</span>
                    {editMode && (
                        <button
                            className="btn btn--secondary btn--sm"
                            onClick={() => setShowAdd(true)}
                        >
                            Загрузить первую главу
                        </button>
                    )}
                </div>
            ) : (
                <div className="ch-items">
                    {sorted.map((ch) => (
                        <div
                            key={ch.id}
                            className={`ch-item${
                                loadingChapter === ch.id
                                    ? ' ch-item--loading'
                                    : ''
                            }`}
                            onClick={() => handleRead(ch)}
                        >
                            {/* Иконка */}
                            <div className="ch-item__icon">
                                {loadingChapter === ch.id ? (
                                    <span className="ch-item__spinner" />
                                ) : (
                                    <span
                                        style={{
                                            opacity: 0.4,
                                            fontSize: 16,
                                        }}
                                    >
                                        👁
                                    </span>
                                )}
                            </div>

                            {/* Название */}
                            <div className="ch-item__title">
                                <span className="ch-item__num">
                                    Глава {ch.chapterNumber}
                                </span>
                                {ch.title && (
                                    <span className="ch-item__name">
                                        — {ch.title}
                                    </span>
                                )}
                            </div>

                            {/* Страниц */}
                            <div className="ch-item__pages">
                                {ch.pageCount} стр.
                            </div>

                            {/* Дата */}
                            <div className="ch-item__date">
                                {formatDate(ch.createdAt)}
                            </div>

                            {editMode && (
                                <button
                                    className="ch-item__delete"
                                    onClick={(e) => askDelete(e, ch)}
                                    title="Удалить главу"
                                >
                                    🗑️
                                </button>
                            )}
                        </div>
                    ))}
                </div>
            )}

            {/* Модалка добавления */}
            {showAdd && (
                <AddChapterModal
                    comicId={comicId}
                    existingNumbers={existingNumbers}
                    onDone={() => {
                        setShowAdd(false);
                        load();
                        showAlert('success', '✅ Глава добавлена!');
                    }}
                    onCancel={() => setShowAdd(false)}
                />
            )}

            {/* Читалка */}
            {readingChapter && (
                <ChapterReader
                    chapter={readingChapter}
                    allChapters={sorted}
                    onClose={() => setReadingChapter(null)}
                    onChapterChange={handleChapterChange}
                />
            )}

            {/* Подтверждение удаления главы */}
            {confirmDelete && (
                <div
                    className="chapter-modal-overlay"
                    onClick={() => setConfirmDelete(null)}
                >
                    <div
                        className="chapter-modal"
                        onClick={(e) => e.stopPropagation()}
                        style={{ maxWidth: 460 }}
                    >
                        <div className="chapter-modal__header">
                            <h3>🗑️ Удалить главу?</h3>
                            <button
                                className="modal__close"
                                onClick={() => setConfirmDelete(null)}
                            >
                                ✕
                            </button>
                        </div>
                        <div className="chapter-modal__body">
                            <p
                                style={{
                                    color: 'var(--text-secondary)',
                                    marginBottom: 8,
                                }}
                            >
                                Вы уверены, что хотите удалить{' '}
                                <strong
                                    style={{
                                        color: 'var(--text-primary)',
                                    }}
                                >
                                    Главу {confirmDelete.chapterNumber}
                                    {confirmDelete.title &&
                                        ` — ${confirmDelete.title}`}
                                </strong>
                                ?
                            </p>
                            <p
                                style={{
                                    color: 'var(--text-muted)',
                                    fontSize: 13,
                                    marginBottom: 20,
                                }}
                            >
                                Будут удалены все {confirmDelete.pageCount}{' '}
                                страниц. Это действие необратимо.
                            </p>
                            <div
                                style={{
                                    display: 'flex',
                                    gap: 12,
                                    justifyContent: 'flex-end',
                                }}
                            >
                                <button
                                    className="btn btn--ghost"
                                    onClick={() => setConfirmDelete(null)}
                                >
                                    Отмена
                                </button>
                                <button
                                    className="btn btn--danger"
                                    onClick={confirmDeleteChapter}
                                >
                                    Удалить навсегда
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}