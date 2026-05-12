// manga-client/src/pages/PublishersPage.jsx
import { useState } from 'react';
import { publishersApi } from '../api/api.js';
import { useApi } from '../hooks/useApi.js';
import Modal from '../components/ui/Modal.jsx';
import Spinner from '../components/ui/Spinner.jsx';

function PublisherModal({ initial, onSubmit, onCancel, loading }) {
    const [name, setName] = useState(initial?.name || '');
    const [error, setError] = useState('');

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!name.trim()) { setError('Введите название'); return; }
        onSubmit({ name: name.trim() });
    };

    return (
        <form onSubmit={handleSubmit}>
            <div className="form-group">
                <label className="form-label">
                    Название <span className="required">*</span>
                </label>
                <input
                    className="form-input"
                    placeholder="Например: Shueisha"
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

export default function PublishersPage() {
    const { data: publishers, loading, refetch } = useApi(publishersApi.getAll);
    const [modal, setModal] = useState(null);
    const [saving, setSaving] = useState(false);
    const [alert, setAlert] = useState(null);
    const [search, setSearch] = useState('');
    const [deleteTarget, setDeleteTarget] = useState(null);

    const showAlert = (type, text) => {
        setAlert({ type, text });
        setTimeout(() => setAlert(null), 3000);
    };

    const handleCreate = async (data) => {
        setSaving(true);
        try {
            await publishersApi.create(data);
            setModal(null);
            showAlert('success', '✅ Издатель добавлен!');
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
            await publishersApi.update(modal.publisher.id, data);
            setModal(null);
            showAlert('success', '✅ Издатель обновлён!');
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
            await publishersApi.delete(deleteTarget.id);
            showAlert('error', `🗑️ Издатель «${deleteTarget.name}» удалён`);
            refetch();
        } catch (err) {
            showAlert('error', `❌ ${err.message}`);
        } finally {
            setDeleteTarget(null);
        }
    };

    const filtered = publishers?.filter((p) =>
        p.name.toLowerCase().includes(search.toLowerCase())
    ) || [];

    return (
        <div>
            <div className="page-header">
                <div>
                    <h1 className="page-title">
                        <span className="page-title__icon">🏢</span> Издатели
                    </h1>
                    <p className="page-subtitle">Всего: {publishers?.length ?? 0}</p>
                </div>
                <button
                    className="btn btn--primary"
                    onClick={() => setModal({ type: 'create' })}
                >
                    ➕ Добавить
                </button>
            </div>

            {alert && (
                <div className={`alert alert--${alert.type}`}>{alert.text}</div>
            )}

            <div style={{ marginBottom: 20 }}>
                <input
                    className="filter-input"
                    placeholder="🔍 Поиск..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    style={{ maxWidth: 320 }}
                />
            </div>

            {loading ? (
                <Spinner />
            ) : (
                <div style={{
                    background: 'var(--bg-secondary)',
                    border: '1px solid var(--border)',
                    borderRadius: 'var(--radius)',
                    overflow: 'hidden',
                }}>
                    <table className="data-table">
                        <thead>
                        <tr>
                            <th>#</th>
                            <th>Издатель</th>
                            <th>Действия</th>
                        </tr>
                        </thead>
                        <tbody>
                        {filtered.length === 0 ? (
                            <tr>
                                <td colSpan={3} style={{ textAlign: 'center', padding: 40 }}>
                                    <span style={{ color: 'var(--text-muted)' }}>Издатели не найдены</span>
                                </td>
                            </tr>
                        ) : (
                            filtered.map((pub) => (
                                <tr key={pub.id}>
                                    <td style={{ color: 'var(--text-muted)', width: 60 }}>
                                        #{pub.id}
                                    </td>
                                    <td>
                                        <strong style={{ color: 'var(--text-primary)' }}>
                                            {pub.name}
                                        </strong>
                                    </td>
                                    <td>
                                        <div style={{ display: 'flex', gap: 8 }}>
                                            <button
                                                className="btn btn--secondary btn--sm"
                                                onClick={() => setModal({ type: 'edit', publisher: pub })}
                                            >
                                                ✏️
                                            </button>
                                            <button
                                                className="btn btn--danger btn--sm"
                                                onClick={() => setDeleteTarget(pub)}
                                            >
                                                🗑️
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                </div>
            )}

            {modal?.type === 'create' && (
                <Modal title="➕ Добавить издателя" onClose={() => setModal(null)}>
                    <PublisherModal
                        onSubmit={handleCreate}
                        onCancel={() => setModal(null)}
                        loading={saving}
                    />
                </Modal>
            )}
            {modal?.type === 'edit' && (
                <Modal title="✏️ Редактировать" onClose={() => setModal(null)}>
                    <PublisherModal
                        initial={modal.publisher}
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
                            Вы уверены, что хотите удалить издателя{' '}
                            <strong style={{ color: 'var(--text-primary)' }}>
                                «{deleteTarget.name}»
                            </strong>?
                        </>
                    }
                    description="Это действие необратимо. Все тайтлы этого издателя останутся в каталоге без привязки к издателю."
                    onConfirm={handleDeleteConfirm}
                    onCancel={() => setDeleteTarget(null)}
                />
            )}
        </div>
    );
}