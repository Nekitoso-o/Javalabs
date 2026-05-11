// manga-client/src/pages/CatalogPage.jsx
import { useState, useEffect, useCallback, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { comicsApi, genresApi, authorsApi } from '../api/api.js';
import { useApi } from '../hooks/useApi.js';
import ComicCard from '../components/ui/ComicCard.jsx';
import Modal from '../components/ui/Modal.jsx';
import ComicForm from '../components/forms/ComicForm.jsx';
import Spinner from '../components/ui/Spinner.jsx';

// --- Кастомный прокручиваемый выпадающий список ---
function CustomDropdown({ value, onChange, options, placeholder, className }) {
    const [isOpen, setIsOpen] = useState(false);
    const ref = useRef(null);

    useEffect(() => {
        const handleClick = (e) => {
            if (ref.current && !ref.current.contains(e.target)) setIsOpen(false);
        };
        document.addEventListener('mousedown', handleClick);
        return () => document.removeEventListener('mousedown', handleClick);
    }, []);

    const selected = options.find((o) => String(o.value) === String(value));

    return (
        <div ref={ref} style={{ position: 'relative', width: '100%' }}>
            <style>{`
                .cd-item { padding: 8px 12px; cursor: pointer; color: var(--text-primary); transition: background 0.1s; }
                .cd-item:hover { background: rgba(255,255,255,0.05); }
                .cd-item.active { background: var(--accent); color: #fff; }
            `}</style>
            <div
                className={className}
                style={{ cursor: 'pointer', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
                onClick={() => setIsOpen(!isOpen)}
            >
                <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {selected ? selected.label : placeholder}
                </span>
                <span style={{ fontSize: 10, opacity: 0.5, transform: isOpen ? 'rotate(180deg)' : 'none', transition: '0.2s' }}>▼</span>
            </div>

            {isOpen && (
                <div
                    style={{
                        position: 'absolute', top: 'calc(100% + 4px)', left: 0, right: 0,
                        maxHeight: 220, overflowY: 'auto',
                        background: 'var(--bg-secondary)', border: '1px solid var(--border)',
                        borderRadius: 'var(--radius)', zIndex: 9999,
                        boxShadow: '0 8px 24px rgba(0,0,0,0.5)',
                        display: 'flex', flexDirection: 'column'
                    }}
                >
                    <div
                        className="cd-item"
                        onClick={() => { onChange(''); setIsOpen(false); }}
                        style={{ color: 'var(--text-muted)', borderBottom: '1px solid var(--border)' }}
                    >
                        {placeholder}
                    </div>
                    {options.map((o) => (
                        <div
                            key={o.value}
                            className={`cd-item ${String(value) === String(o.value) ? 'active' : ''}`}
                            onClick={() => { onChange(o.value); setIsOpen(false); }}
                        >
                            {o.label}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
// --------------------------------------------------

export default function CatalogPage() {
    const [searchParams, setSearchParams] = useSearchParams();

    const [comics, setComics] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const [titleFilter, setTitleFilter] = useState(searchParams.get('search') || '');
    const [genreFilter, setGenreFilter] = useState(searchParams.get('genre') || '');
    const [yearFilter, setYearFilter] = useState(searchParams.get('year') || '');
    const [authorFilterId, setAuthorFilterId] = useState(searchParams.get('authorId') || '');
    const [authorFilterName, setAuthorFilterName] = useState(searchParams.get('authorName') || '');

    const [showModal, setShowModal] = useState(false);
    const [saving, setSaving] = useState(false);
    const [alert, setAlert] = useState(null);

    const { data: genres } = useApi(genresApi.getAll);
    const { data: authors } = useApi(authorsApi.getAll);

    const loadComics = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            let data;
            if (authorFilterId) {
                data = await comicsApi.getByAuthor(authorFilterId);
            } else if (genreFilter || yearFilter) {
                data = await comicsApi.complexSearch({
                    genreName: genreFilter || undefined,
                    minYear: yearFilter ? Number(yearFilter) : undefined,
                    page: 0,
                    size: 200,
                });
            } else if (titleFilter) {
                data = await comicsApi.search(titleFilter);
            } else {
                data = await comicsApi.getAll();
            }
            setComics(data);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Ошибка загрузки');
        } finally {
            setLoading(false);
        }
    }, [titleFilter, genreFilter, yearFilter, authorFilterId]);

    useEffect(() => {
        const timer = setTimeout(loadComics, 300);
        return () => clearTimeout(timer);
    }, [loadComics]);

    useEffect(() => {
        const params = {};
        if (authorFilterId) {
            params.authorId = authorFilterId;
            if (authorFilterName) params.authorName = authorFilterName;
        } else {
            if (titleFilter) params.search = titleFilter;
            if (genreFilter) params.genre = genreFilter;
            if (yearFilter) params.year = yearFilter;
        }
        setSearchParams(params, { replace: true });
    }, [titleFilter, genreFilter, yearFilter, authorFilterId, authorFilterName, setSearchParams]);

    const handleCreate = async (formData) => {
        setSaving(true);
        try {
            await comicsApi.create(formData);
            setShowModal(false);
            setAlert({ type: 'success', text: '✅ Комикс успешно добавлен!' });
            loadComics();
            setTimeout(() => setAlert(null), 3000);
        } catch (err) {
            setAlert({ type: 'error', text: `❌ ${err instanceof Error ? err.message : 'Ошибка'}` });
        } finally {
            setSaving(false);
        }
    };

    const clearFilters = () => {
        setTitleFilter('');
        setGenreFilter('');
        setYearFilter('');
        setAuthorFilterId('');
        setAuthorFilterName('');
    };

    const handleAuthorChange = (selectedId) => {
        if (!selectedId) {
            setAuthorFilterId('');
            setAuthorFilterName('');
        } else {
            const found = authors?.find((a) => String(a.id) === String(selectedId));
            setAuthorFilterId(selectedId);
            setAuthorFilterName(found?.name || '');
            setTitleFilter('');
            setGenreFilter('');
            setYearFilter('');
        }
    };

    const handleTitleChange = (v) => {
        setTitleFilter(v);
        if (authorFilterId) { setAuthorFilterId(''); setAuthorFilterName(''); }
    };

    const handleGenreChange = (v) => {
        setGenreFilter(v);
        if (authorFilterId) { setAuthorFilterId(''); setAuthorFilterName(''); }
    };

    const handleYearChange = (v) => {
        setYearFilter(v);
        if (authorFilterId) { setAuthorFilterId(''); setAuthorFilterName(''); }
    };

    const hasFilters = titleFilter || genreFilter || yearFilter || authorFilterId;

    let subtitle = '';
    if (authorFilterId && authorFilterName) {
        subtitle = `Тайтлы автора: ${authorFilterName}`;
    } else if (comics.length > 0) {
        subtitle = `Найдено тайтлов: ${comics.length}`;
    }

    return (
        <div>
            <div className="page-header">
                <div>
                    <h1 className="page-title">
                        <span className="page-title__icon">📚</span> Каталог
                    </h1>
                    {subtitle && <p className="page-subtitle">{subtitle}</p>}
                </div>
                <button className="btn btn--primary" onClick={() => setShowModal(true)}>
                    ➕ Добавить тайтл
                </button>
            </div>

            {alert && (
                <div className={`alert alert--${alert.type}`}>{alert.text}</div>
            )}

            {/* ── Фильтры ── */}
            <div className="filters">
                <div className="filter-item" style={{ flex: 2, minWidth: 180 }}>
                    <div className="filter-label">🔍 Название</div>
                    <input
                        className="filter-input"
                        placeholder="Введите название..."
                        value={titleFilter}
                        onChange={(e) => handleTitleChange(e.target.value)}
                        style={{ width: '100%' }}
                    />
                </div>

                <div className="filter-item" style={{ minWidth: 160 }}>
                    <div className="filter-label">✍️ Автор</div>
                    <CustomDropdown
                        className="filter-select"
                        value={authorFilterId}
                        onChange={handleAuthorChange}
                        placeholder="Все авторы"
                        options={authors?.map((a) => ({ value: a.id, label: a.name })) || []}
                    />
                </div>

                <div className="filter-item" style={{ minWidth: 140 }}>
                    <div className="filter-label">🏷️ Жанр</div>
                    <CustomDropdown
                        className="filter-select"
                        value={genreFilter}
                        onChange={handleGenreChange}
                        placeholder="Все жанры"
                        options={genres?.map((g) => ({ value: g.name, label: g.name })) || []}
                    />
                </div>

                <div className="filter-item" style={{ minWidth: 90 }}>
                    <div className="filter-label">📅 Год от</div>
                    <input
                        className="filter-input"
                        type="number"
                        placeholder="Любой"
                        min="1930"
                        max="2026"
                        value={yearFilter}
                        onChange={(e) => handleYearChange(e.target.value)}
                    />
                </div>

                {hasFilters && (
                    <button
                        className="btn btn--ghost btn--sm"
                        onClick={clearFilters}
                        style={{ alignSelf: 'flex-end' }}
                    >
                        ✕ Сбросить
                    </button>
                )}
            </div>

            {/* ── Результаты ── */}
            {loading ? (
                <Spinner />
            ) : error ? (
                <div className="alert alert--error">❌ {error}</div>
            ) : comics.length === 0 ? (
                <div className="empty-state">
                    <div className="empty-state__icon">🔍</div>
                    <div className="empty-state__title">Ничего не найдено</div>
                    <p className="empty-state__text">Попробуйте изменить параметры поиска</p>
                </div>
            ) : (
                <div className="comic-grid">
                    {comics.map((comic) => (
                        <ComicCard key={comic.id} comic={comic} />
                    ))}
                </div>
            )}

            {showModal && (
                <Modal title="➕ Добавить комикс" onClose={() => setShowModal(false)}>
                    <ComicForm
                        onSubmit={handleCreate}
                        onCancel={() => setShowModal(false)}
                        loading={saving}
                    />
                </Modal>
            )}
        </div>
    );
}