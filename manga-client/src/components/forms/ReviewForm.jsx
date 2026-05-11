import { useState } from 'react';

export default function ReviewForm({ comicId, onSubmit, onCancel, loading }) {
    const [form, setForm] = useState({ text: '', rating: 7 });
    const [errors, setErrors] = useState({});

    const validate = () => {
        const errs = {};
        if (!form.text.trim()) errs.text = 'Введите текст отзыва';
        if (form.rating < 1 || form.rating > 10) errs.rating = 'Оценка от 1 до 10';
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
            text: form.text.trim(),
            rating: Number(form.rating),
            comicId: Number(comicId),
        });
    };

    return (
        <form onSubmit={handleSubmit}>
            <div className="form-group">
                <label className="form-label">Ваш отзыв</label>
                <textarea
                    className="form-textarea"
                    placeholder="Поделитесь впечатлениями о произведении..."
                    rows={4}
                    value={form.text}
                    onChange={(e) => setForm({ ...form, text: e.target.value })}
                />
                {errors.text && <div className="form-error">{errors.text}</div>}
            </div>

            <div className="form-group">
                <label className="form-label">
                    Оценка: <strong style={{ color: 'var(--warning)', fontSize: '18px' }}>
                    {form.rating}
                </strong> / 10
                </label>
                <input
                    type="range"
                    min="1"
                    max="10"
                    step="1"
                    value={form.rating}
                    onChange={(e) => setForm({ ...form, rating: Number(e.target.value) })}
                    style={{ width: '100%', accentColor: 'var(--accent)', marginTop: '4px' }}
                />
                <div
                    style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        fontSize: '11px',
                        color: 'var(--text-muted)',
                    }}
                >
                    <span>1 — Плохо</span>
                    <span>5 — Средне</span>
                    <span>10 — Шедевр</span>
                </div>
                {errors.rating && <div className="form-error">{errors.rating}</div>}
            </div>

            <div
                style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}
            >
                <button type="button" className="btn btn--ghost" onClick={onCancel}>
                    Отмена
                </button>
                <button type="submit" className="btn btn--primary" disabled={loading}>
                    {loading ? '⏳...' : '📝 Оставить отзыв'}
                </button>
            </div>
        </form>
    );
}