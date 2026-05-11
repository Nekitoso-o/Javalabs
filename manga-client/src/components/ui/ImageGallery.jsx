import { useState, useEffect } from 'react';
import { comicImagesApi } from '../../api/api.js';

export default function ImageGallery({ comicId, editable = false, onUpdate }) {
    const [images, setImages] = useState([]);
    const [activeIdx, setActiveIdx] = useState(0);

    // Очередь изменений
    const [pendingAdd, setPendingAdd] = useState([]);
    const [pendingDelete, setPendingDelete] = useState([]);

    const [dragOver, setDragOver] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const load = () => {
        comicImagesApi.getAll(comicId).then(setImages).catch(() => {});
    };

    useEffect(() => { load(); }, [comicId]);

    const hasPending = pendingAdd.length > 0 || pendingDelete.length > 0;

    // Все картинки для отображения
    const displayImages = [
        ...images.map((img) => ({
            ...img,
            isPending: false,
            isMarkedDelete: pendingDelete.includes(img.id),
        })),
        ...pendingAdd.map((p) => ({
            id: p.id,
            url: p.previewUrl,
            originalName: p.name,
            isPending: true,
            isMarkedDelete: false,
        })),
    ];

    const safeIdx = displayImages.length > 0
        ? Math.min(activeIdx, displayImages.length - 1)
        : 0;
    const active = displayImages[safeIdx] || null;

    const handleFiles = (files) => {
        setError('');
        const valid = Array.from(files).filter((f) => f.type.startsWith('image/'));
        if (!valid.length) { setError('Выберите изображения (JPG, PNG, WebP)'); return; }
        const previews = valid.map((file) => ({
            id: `pending-${Date.now()}-${Math.random()}`,
            file,
            previewUrl: URL.createObjectURL(file),
            name: file.name,
        }));
        setPendingAdd((prev) => [...prev, ...previews]);
    };

    const markDelete = (imgId, e) => {
        e.stopPropagation();
        setPendingDelete((prev) =>
            prev.includes(imgId) ? prev.filter((i) => i !== imgId) : [...prev, imgId]
        );
    };

    const removePending = (pendingId, e) => {
        e.stopPropagation();
        setPendingAdd((prev) => {
            const item = prev.find((p) => p.id === pendingId);
            if (item) URL.revokeObjectURL(item.previewUrl);
            return prev.filter((p) => p.id !== pendingId);
        });
    };

    const handleSetMain = async (imgId, e) => {
        e.stopPropagation();
        const idx = images.findIndex((i) => i.id === imgId);
        if (idx <= 0) return;
        const newOrder = [images[idx], ...images.slice(0, idx), ...images.slice(idx + 1)];
        try {
            const updated = await comicImagesApi.reorder(comicId, newOrder.map((i) => i.id));
            setImages(updated);
            setActiveIdx(0);
        } catch (e) { setError(e.message); }
    };

    const handleSave = async () => {
        setSaving(true);
        setError('');
        setSuccess('');
        try {
            for (const imgId of pendingDelete) {
                await comicImagesApi.delete(comicId, imgId);
            }
            if (pendingAdd.length > 0) {
                await comicImagesApi.upload(comicId, pendingAdd.map((p) => p.file));
                pendingAdd.forEach((p) => URL.revokeObjectURL(p.previewUrl));
            }
            setPendingDelete([]);
            setPendingAdd([]);
            setActiveIdx(0);
            setSuccess('✅ Сохранено!');
            setTimeout(() => setSuccess(''), 3000);
            load();
            if (onUpdate) onUpdate();
        } catch (e) {
            setError(e.message || 'Ошибка сохранения');
        } finally {
            setSaving(false);
        }
    };

    const handleCancel = () => {
        pendingAdd.forEach((p) => URL.revokeObjectURL(p.previewUrl));
        setPendingAdd([]);
        setPendingDelete([]);
        setError('');
    };

    return (
        <div className="ig-wrap">
            <div className="ig-layout">
                {/* Левая часть — большой превью */}
                <div
                    className="ig-preview"
                    onDragOver={editable ? (e) => { e.preventDefault(); setDragOver(true); } : undefined}
                    onDragLeave={editable ? () => setDragOver(false) : undefined}
                    onDrop={editable ? (e) => {
                        e.preventDefault(); setDragOver(false);
                        if (e.dataTransfer.files.length) handleFiles(e.dataTransfer.files);
                    } : undefined}
                    style={dragOver ? { borderColor: 'var(--accent)', background: 'rgba(233,69,96,0.06)' } : undefined}
                >
                    {active ? (
                        <>
                            <img
                                src={active.url}
                                alt={active.originalName}
                                className="ig-preview__img"
                                style={{
                                    filter: active.isMarkedDelete ? 'grayscale(1)' : undefined,
                                    opacity: active.isMarkedDelete ? 0.4 : 1,
                                }}
                            />
                            {active.isMarkedDelete && (
                                <div className="ig-preview__badge ig-preview__badge--delete">
                                    🗑️ Будет удалено
                                </div>
                            )}
                            {active.isPending && (
                                <div className="ig-preview__badge ig-preview__badge--new">
                                    + Новое
                                </div>
                            )}
                        </>
                    ) : (
                        <div className="ig-preview__empty">
                            <span>🖼️</span>
                            <span>{editable ? 'Перетащите или добавьте фото' : 'Нет изображений'}</span>
                        </div>
                    )}
                </div>

                {/* Правая часть — сетка миниатюр */}
                <div className="ig-thumbs-col">
                    <div className="ig-thumbs-grid">
                        {displayImages.map((img, idx) => (
                            <div
                                key={img.id}
                                className={`ig-thumb${idx === safeIdx ? ' active' : ''}${img.isMarkedDelete ? ' marked-delete' : ''}${img.isPending ? ' pending' : ''}`}
                                onClick={() => setActiveIdx(idx)}
                                title={img.isPending ? 'Новое' : img.isMarkedDelete ? 'Будет удалено' : img.originalName}
                            >
                                <img src={img.url} alt="" />

                                {/* Главная */}
                                {idx === 0 && !img.isMarkedDelete && !img.isPending && (
                                    <div className="ig-thumb__star">✦</div>
                                )}

                                {/* Действия при наведении */}
                                {editable && (
                                    <div className="ig-thumb__overlay">
                                        {!img.isPending && !img.isMarkedDelete && idx !== 0 && (
                                            <button
                                                className="ig-thumb__btn"
                                                title="Сделать главной"
                                                onClick={(e) => handleSetMain(img.id, e)}
                                            >⭐</button>
                                        )}
                                        {!img.isPending && (
                                            <button
                                                className={`ig-thumb__btn${img.isMarkedDelete ? ' ig-thumb__btn--restore' : ' ig-thumb__btn--del'}`}
                                                onClick={(e) => markDelete(img.id, e)}
                                                title={img.isMarkedDelete ? 'Отменить' : 'Удалить'}
                                            >
                                                {img.isMarkedDelete ? '↩' : '✕'}
                                            </button>
                                        )}
                                        {img.isPending && (
                                            <button
                                                className="ig-thumb__btn ig-thumb__btn--del"
                                                onClick={(e) => removePending(img.id, e)}
                                                title="Убрать"
                                            >✕</button>
                                        )}
                                    </div>
                                )}
                            </div>
                        ))}

                        {/* Кнопка добавления — ячейка в сетке */}
                        {editable && (
                            <label className="ig-thumb ig-thumb--add" title="Добавить изображение">
                                <input
                                    type="file" accept="image/*" multiple
                                    style={{ display: 'none' }}
                                    onChange={(e) => {
                                        if (e.target.files?.length) handleFiles(e.target.files);
                                        e.target.value = '';
                                    }}
                                />
                                <span className="ig-thumb__add-icon">+</span>
                                <span className="ig-thumb__add-label">Добавить</span>
                            </label>
                        )}
                    </div>

                    {/* Подсказка */}
                    {editable && (
                        <div className="form-hint" style={{ marginTop: 8 }}>
                            JPG, PNG, WebP · до 20 MB
                        </div>
                    )}
                </div>
            </div>

            {/* Сообщения */}
            {error && <div className="form-error" style={{ marginTop: 8 }}>{error}</div>}
            {success && (
                <div style={{ color: 'var(--success)', fontSize: 13, marginTop: 8 }}>{success}</div>
            )}

            {/* Кнопки сохранить/отменить */}
            {editable && hasPending && (
                <div className="ig-actions">
                    <div className="ig-actions__info">
                        {pendingAdd.length > 0 && (
                            <span style={{ color: 'var(--success)' }}>+{pendingAdd.length} новых</span>
                        )}
                        {pendingDelete.length > 0 && (
                            <span style={{ color: 'var(--danger)' }}>−{pendingDelete.length} удалить</span>
                        )}
                    </div>
                    <div style={{ display: 'flex', gap: 8 }}>
                        <button className="btn btn--ghost btn--sm" onClick={handleCancel} disabled={saving}>
                            Отмена
                        </button>
                        <button className="btn btn--primary btn--sm" onClick={handleSave} disabled={saving}>
                            {saving ? '⏳ Сохранение...' : '💾 Сохранить'}
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}