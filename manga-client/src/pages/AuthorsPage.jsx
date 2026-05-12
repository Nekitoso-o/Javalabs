// manga-client/src/pages/AuthorsPage.jsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authorsApi } from '../api/api.js';
import { useApi } from '../hooks/useApi.js';
import Modal from '../components/ui/Modal.jsx';
import Spinner from '../components/ui/Spinner.jsx';

function AuthorModal({ initial, onSubmit, onCancel, loading }) {
    const [name, setName] = useState(initial?.name || '');
    const [error, setError] = useState('');

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!name.trim()) { setError('Введите имя автора'); return; }
        onSubmit({ name: name.trim() });
    };

    return (
        <form onSubmit={handleSubmit}>
            <div className="form-group">
                <label className="form-label">
                    Имя автора <span className="required">*</span>
                </label>
                <input
                    className="form-input"
                    placeholder="Например: Кэнтаро Миура"
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

export default function AuthorsPage() {
    const navigate = useNavigate();
    const { data: authors, loading, refetch } = useApi(authorsApi.getAll);
    const [modal, setModal] = useState(null);
    const [saving, setSaving] = useState(false);
    const [alert, setAlert] = useState(null);
    const [search, setSearch] = useState('');
    const [deleteTarget, setDeleteTarget] = useState(null); // автор для удаления

    const showAlert = (type, text) => {
        setAlert({ type, text });
        setTimeout(() => setAlert(null), 3000);
    };

    const handleCreate = async (data) => {
        setSaving(true);
        try {
            await authorsApi.create(data);
            setModal(null);
            showAlert('success', '✅ Автор добавлен!');
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
            await authorsApi.update(modal.author.id, data);
            setModal(null);
            showAlert('success', '✅ Автор обновлён!');
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
            await authorsApi.delete(deleteTarget.id);
            showAlert('error', `🗑️ Автор «${deleteTarget.name}» удалён`);
            refetch();
        } catch (err) {
            showAlert('error', `❌ ${err.message}`);
        } finally {
            setDeleteTarget(null);
        }
    };

    const filtered = authors?.filter((a) =>
        a.name.toLowerCase().includes(search.toLowerCase())
    ) || [];

    return (
        <div>
            <div className="page-header">
                <div>
                    <h1 className="page-title">
                        <span className="page-title__icon">✍️</span> Авторы
                    </h1>
                    <p className="page-subtitle">Всего: {authors?.length ?? 0}</p>
                </div>
                <button
                    className="btn btn--primary"
                    onClick={() => setModal({ type: 'create' })}
                >
                    ➕ Добавить автора
                </button>
            </div>

            {alert && (
                <div className={`alert alert--${alert.type}`}>{alert.text}</div>
            )}

            <div style={{ marginBottom: 20 }}>
                <input
                    className="filter-input"
                    placeholder="🔍 Поиск по имени..."
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
                            <th>Имя автора</th>
                            <th>Действия</th>
                        </tr>
                        </thead>
                        <tbody>
                        {filtered.length === 0 ? (
                            <tr>
                                <td colSpan={3} style={{ textAlign: 'center', padding: 40 }}>
                                    <span style={{ color: 'var(--text-muted)' }}>Авторы не найдены</span>
                                </td>
                            </tr>
                        ) : (
                            filtered.map((author) => (
                                <tr key={author.id}>
                                    <td style={{ color: 'var(--text-muted)', width: 60 }}>
                                        #{author.id}
                                    </td>
                                    <td>
                                        <strong style={{ color: 'var(--text-primary)' }}>
                                            {author.name}
                                        </strong>
                                    </td>
                                    <td>
                                        <div style={{ display: 'flex', gap: 8 }}>
                                            <button
                                                className="btn btn--ghost btn--sm"
                                                onClick={() => navigate(`/catalog?authorId=${author.id}&authorName=${encodeURIComponent(author.name)}`)}
                                            >
                                                📚 Тайтлы
                                            </button>
                                            <button
                                                className="btn btn--secondary btn--sm"
                                                onClick={() => setModal({ type: 'edit', author })}
                                            >
                                                ✏️
                                            </button>
                                            <button
                                                className="btn btn--danger btn--sm"
                                                onClick={() => setDeleteTarget(author)}
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

            {/* Модалки создания/редактирования */}
            {modal?.type === 'create' && (
                <Modal title="➕ Добавить автора" onClose={() => setModal(null)}>
                    <AuthorModal
                        onSubmit={handleCreate}
                        onCancel={() => setModal(null)}
                        loading={saving}
                    />
                </Modal>
            )}
            {modal?.type === 'edit' && (
                <Modal title="✏️ Редактировать автора" onClose={() => setModal(null)}>
                    <AuthorModal
                        initial={modal.author}
                        onSubmit={handleUpdate}
                        onCancel={() => setModal(null)}
                        loading={saving}
                    />
                </Modal>
            )}

            {/* Модалка подтверждения удаления */}
            {deleteTarget && (
                <DeleteConfirmModal
                    title={
                        <>
                            Вы уверены, что хотите удалить автора{' '}
                            <strong style={{ color: 'var(--text-primary)' }}>
                                «{deleteTarget.name}»
                            </strong>?
                        </>
                    }
                    description="Это действие необратимо. Все тайтлы этого автора останутся в каталоге без привязки к автору."
                    onConfirm={handleDeleteConfirm}
                    onCancel={() => setDeleteTarget(null)}
                />
            )}
        </div>
    );
}