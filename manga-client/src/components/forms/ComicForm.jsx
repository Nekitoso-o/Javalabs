// manga-client/src/components/forms/ComicForm.jsx
import { useState, useEffect, useRef } from 'react';
import { authorsApi, publishersApi, genresApi } from '../../api/api.js';
import { useApi } from '../../hooks/useApi.js';

const CURRENT_YEAR = new Date().getFullYear();

const defaultState = {
    title: '',
    releaseYear: CURRENT_YEAR,
    authorId: '',
    publisherId: '',
    genreIds: [],
};

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

export default function ComicForm({ initial, onSubmit, onCancel, loading }) {
    const [form, setForm] = useState(defaultState);
    const [errors, setErrors] = useState({});

    const { data: authors } = useApi(authorsApi.getAll);
    const { data: publishers } = useApi(publishersApi.getAll);
    const { data: genres } = useApi(genresApi.getAll);

    useEffect(() => {
        if (initial) {
            setForm({
                title: initial.title || '',
                releaseYear: initial.releaseYear || CURRENT_YEAR,
                authorId: initial.author?.id || '',
                publisherId: initial.publisher?.id || '',
                genreIds: initial.genres?.map((g) => g.id) || [],
            });
        }
    }, [initial]);

    const validate = () => {
        const errs = {};
        if (!form.title.trim()) errs.title = 'Название обязательно';
        if (!form.authorId) errs.authorId = 'Выберите автора';
        if (!form.publisherId) errs.publisherId = 'Выберите издателя';
        if (form.genreIds.length === 0) errs.genreIds = 'Выберите хотя бы один жанр';
        if (form.releaseYear < 1930 || form.releaseYear > 2026)
            errs.releaseYear = 'Некорректный год (1930–2026)';
        return errs;
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        const errs = validate();
        if (Object.keys(errs).length > 0) {
            setErrors(errs);
            return;
        }
        setErrors({});
        onSubmit({
            title: form.title.trim(),
            releaseYear: Number(form.releaseYear),
            authorId: Number(form.authorId),
            publisherId: Number(form.publisherId),
            genreIds: form.genreIds,
        });
    };

    const toggleGenre = (id) => {
        setForm((prev) => ({
            ...prev,
            genreIds: prev.genreIds.includes(id)
                ? prev.genreIds.filter((gId) => gId !== id)
                : [...prev.genreIds, id],
        }));
    };

    return (
        <form onSubmit={handleSubmit}>
            <div className="form-group">
                <label className="form-label">
                    Название <span className="required">*</span>
                </label>
                <input
                    className="form-input"
                    placeholder="Например: Берсерк"
                    value={form.title}
                    onChange={(e) => setForm({ ...form, title: e.target.value })}
                />
                {errors.title && <div className="form-error">{errors.title}</div>}
            </div>

            <div className="form-group">
                <label className="form-label">
                    Год выпуска <span className="required">*</span>
                </label>
                <input
                    className="form-input"
                    type="number"
                    min="1930"
                    max="2026"
                    value={form.releaseYear}
                    onChange={(e) => setForm({ ...form, releaseYear: e.target.value })}
                />
                {errors.releaseYear && <div className="form-error">{errors.releaseYear}</div>}
            </div>

            <div className="form-group">
                <label className="form-label">
                    Автор <span className="required">*</span>
                </label>
                <CustomDropdown
                    className="form-select"
                    value={form.authorId}
                    onChange={(val) => setForm({ ...form, authorId: val })}
                    placeholder="— Выберите автора —"
                    options={authors?.map((a) => ({ value: a.id, label: a.name })) || []}
                />
                {errors.authorId && <div className="form-error">{errors.authorId}</div>}
            </div>

            <div className="form-group">
                <label className="form-label">
                    Издатель <span className="required">*</span>
                </label>
                <CustomDropdown
                    className="form-select"
                    value={form.publisherId}
                    onChange={(val) => setForm({ ...form, publisherId: val })}
                    placeholder="— Выберите издателя —"
                    options={publishers?.map((p) => ({ value: p.id, label: p.name })) || []}
                />
                {errors.publisherId && <div className="form-error">{errors.publisherId}</div>}
            </div>

            <div className="form-group">
                <label className="form-label">
                    Жанры <span className="required">*</span>
                </label>
                {/* Ограничиваем высоту блока жанров и добавляем скролл */}
                <div className="genres-picker" style={{ maxHeight: '180px', overflowY: 'auto', paddingRight: '4px' }}>
                    {genres?.map((g) => (
                        <button
                            type="button"
                            key={g.id}
                            className={`genre-chip${form.genreIds.includes(g.id) ? ' selected' : ''}`}
                            onClick={() => toggleGenre(g.id)}
                        >
                            {g.name}
                        </button>
                    ))}
                </div>
                {errors.genreIds && <div className="form-error">{errors.genreIds}</div>}
                <div className="form-hint">Выбрано: {form.genreIds.length}</div>
            </div>

            <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end', marginTop: '8px' }}>
                <button type="button" className="btn btn--ghost" onClick={onCancel}>
                    Отмена
                </button>
                <button type="submit" className="btn btn--primary" disabled={loading}>
                    {loading ? '⏳ Сохранение...' : '💾 Сохранить'}
                </button>
            </div>
        </form>
    );
}