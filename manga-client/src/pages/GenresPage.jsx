// manga-client/src/pages/GenresPage.jsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { genresApi } from '../api/api.js';
import { useApi } from '../hooks/useApi.js';
import Modal from '../components/ui/Modal.jsx';
import Badge from '../components/ui/Badge.jsx';
import Spinner from '../components/ui/Spinner.jsx';

function GenreModal({ initial, onSubmit, onCancel, loading }) {
    const [name, setName] = useState(initial?.name || '');
    const [error, setError] = useState('');

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!name.trim()) { setError('Введите название жанра'); return; }
        onSubmit({ name: name.trim() });
    };

    return (
        <form onSubmit={handleSubmit}>
            <div className="form-group">
                <label className="form-label">
                    Жанр <span className="required">*</span>
                </label>
                <input
                    className="form-input"
                    placeholder="Например: Сёнэн"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    autoFocus
                />
                {error && <div className="form-error">{error}</div>}
            </div>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
                <button type="button" className="btn btn--ghost" onClick={onCancel}>
                    Отмена
                </button>
                <button type="submit" className="btn btn--primary" disabled={loading}>
                    {loading ? '⏳...' : '💾 Сохранить'}
                </button>
            </div>
        </form>
    );
}

// --- Модалка подтверждения удаления ---
function DeleteConfirmModal({ title, description, onConfirm, onCancel }) {
    return (
        <Modal title="🗑️ Подтверждение удаления" onClose={onCancel}>
            <p style={{ color: 'var(--text-secondary)', marginBottom: 8 }}>{title}</p>
            {description && (
                <p style={{ color: 'var(--text-muted)', fontSize: 13, marginBottom: 20 }}>
                    {description}
                </p>
            )}
            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
                <button className="btn btn--ghost" onClick={onCancel}>
                    Отмена
                </button>
                <button className="btn btn--danger" onClick={onConfirm}>
                    🗑️ Удалить
                </button>
            </div>
        </Modal>
    );
}

export default function GenresPage() {
    const navigate = useNavigate();
    const { data: genres, loading, refetch } = useApi(genresApi.getAll);
    const [modal, setModal] = useState(null);
    const [saving, setSaving] = useState(false);
    const [alert, setAlert] = useState(null);
    const [deleteTarget, setDeleteTarget] = useState(null);

    const showAlert = (type, text) => {
        setAlert({ type, text });
        setTimeout(() => setAlert(null), 3000);
    };

    const handleCreate = async (data) => {
        setSaving(true);
        try {
            await genresApi.create(data);
            setModal(null);
            showAlert('success', '✅ Жанр добавлен!');
            refetch();
        } catch (err) {
            showAlert('error', `❌ ${err.message}`);
        } finally {
            setSaving(false);
        }
    };

    const handleUpdate = async (data) => {
        setSaving(true);
        try {
            await genresApi.update(modal.genre.id, data);
            setModal(null);
            showAlert('success', '✅ Жанр обновлён!');
            refetch();
        } catch (err) {
            showAlert('error', `❌ ${err.message}`);
        } finally {
            setSaving(false);
        }
    };

    const handleDeleteConfirm = async () => {
        if (!deleteTarget) return;
        try {
            await genresApi.delete(deleteTarget.id);
            showAlert('error', `🗑️ Жанр «${deleteTarget.name}» удалён`);
            refetch();
        } catch (err) {
            showAlert('error', `❌ ${err.message}`);
        } finally {
            setDeleteTarget(null);
        }
    };

    return (
        <div>
            <div className="page-header">
                <div>
                    <h1 className="page-title">
                        <span className="page-title__icon">🏷️</span> Жанры
                    </h1>
                    <p className="page-subtitle">Всего: {genres?.length ?? 0}</p>
                </div>
                <button
                    className="btn btn--primary"
                    onClick={() => setModal({ type: 'create' })}
                >
                    ➕ Добавить жанр
                </button>
            </div>

            {alert && (
                <div className={`alert alert--${alert.type}`}>{alert.text}</div>
            )}

            {loading ? (
                <Spinner />
            ) : (
                <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
                    gap: 16,
                }}>
                    {genres?.map((genre) => (
                        <div
                            key={genre.id}
                            style={{
                                background: 'var(--bg-secondary)',
                                border: '1px solid var(--border)',
                                borderRadius: 'var(--radius)',
                                padding: '20px',
                                display: 'flex',
                                flexDirection: 'column',
                                gap: 12,
                                transition: 'var(--transition)',
                            }}
                        >
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                <Badge>{genre.name}</Badge>
                                <span style={{ color: 'var(--text-muted)', fontSize: 12 }}>
                                    #{genre.id}
                                </span>
                            </div>

                            <div style={{ display: 'flex', gap: 8, marginTop: 'auto' }}>
                                <button
                                    className="btn btn--ghost btn--sm"
                                    style={{ flex: 1 }}
                                    onClick={() => navigate(`/catalog?genre=${encodeURIComponent(genre.name)}`)}
                                >
                                    📚 Смотреть
                                </button>
                                <button
                                    className="btn btn--secondary btn--sm"
                                    onClick={() => setModal({ type: 'edit', genre })}
                                >
                                    ✏️
                                </button>
                                <button
                                    className="btn btn--danger btn--sm"
                                    onClick={() => setDeleteTarget(genre)}
                                >
                                    🗑️
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {modal?.type === 'create' && (
                <Modal title="➕ Новый жанр" onClose={() => setModal(null)}>
                    <GenreModal
                        onSubmit={handleCreate}
                        onCancel={() => setModal(null)}
                        loading={saving}
                    />
                </Modal>
            )}
            {modal?.type === 'edit' && (
                <Modal title="✏️ Редактировать жанр" onClose={() => setModal(null)}>
                    <GenreModal
                        initial={modal.genre}
                        onSubmit={handleUpdate}
                        onCancel={() => setModal(null)}
                        loading={saving}
                    />
                </Modal>
            )}

            {deleteTarget && (
                <DeleteConfirmModal
                    title={
                        <>
                            Вы уверены, что хотите удалить жанр{' '}
                            <strong style={{ color: 'var(--text-primary)' }}>
                                «{deleteTarget.name}»
                            </strong>?
                        </>
                    }
                    description="Это действие необратимо. Жанр будет удалён из всех тайтлов, к которым он привязан."
                    onConfirm={handleDeleteConfirm}
                    onCancel={() => setDeleteTarget(null)}
                />
            )}
        </div>
    );
}