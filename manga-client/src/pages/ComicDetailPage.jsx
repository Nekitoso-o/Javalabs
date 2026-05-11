import { useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { comicsApi, reviewsApi, comicImagesApi } from '../api/api.js';
import { useApi } from '../hooks/useApi.js';
import Badge from '../components/ui/Badge.jsx';
import Rating from '../components/ui/Rating.jsx';
import Modal from '../components/ui/Modal.jsx';
import ComicForm from '../components/forms/ComicForm.jsx';
import ReviewForm from '../components/forms/ReviewForm.jsx';
import Spinner from '../components/ui/Spinner.jsx';
import ImageGallery from '../components/ui/ImageGallery.jsx';
import ChapterList from '../components/ui/ChapterList.jsx';
import CoverLightbox from '../components/ui/CoverLightbox.jsx';


const TABS = ['О тайтле', 'Главы', 'Отзывы', 'Редактировать'];
const COVERS = ['📚','📖','🗡️','⚔️','🌸','🔮','🦊','🐉','💫','🌙'];

export default function ComicDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('О тайтле');
    const [coverLightbox, setCoverLightbox] = useState(false);

    const { data: comic, loading, error, refetch } = useApi(
        useCallback(() => comicsApi.getById(id), [id])
    );
    const { data: reviews, refetch: refetchReviews } = useApi(
        useCallback(() => reviewsApi.getByComicId(id), [id])
    );
    const { data: images, refetch: refetchImages } = useApi(
        useCallback(() => comicImagesApi.getAll(id), [id])
    );

    const [showReviewModal, setShowReviewModal] = useState(false);
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [saving, setSaving] = useState(false);
    const [deletingReview, setDeletingReview] = useState(null);
    const [alert, setAlert] = useState(null);

    const showAlert = (type, text) => {
        setAlert({ type, text });
        setTimeout(() => setAlert(null), 3000);
    };

    const handleUpdate = async (formData) => {
        setSaving(true);
        try {
            await comicsApi.update(id, formData);
            showAlert('success', '✅ Комикс обновлён!');
            refetch();
        } catch (err) {
            showAlert('error', `❌ ${err.message}`);
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async () => {
        try {
            await comicsApi.delete(id);
            navigate('/catalog');
        } catch (err) {
            showAlert('error', `❌ ${err.message}`);
            setShowDeleteConfirm(false);
        }
    };

    const handleAddReview = async (formData) => {
        setSaving(true);
        try {
            await reviewsApi.create(formData);
            setShowReviewModal(false);
            showAlert('success', '✅ Отзыв добавлен!');
            refetchReviews();
        } catch (err) {
            showAlert('error', `❌ ${err.message}`);
        } finally {
            setSaving(false);
        }
    };

    const handleDeleteReview = async (reviewId) => {
        setDeletingReview(reviewId);
        try {
            await reviewsApi.delete(reviewId);
            showAlert('success', '✅ Отзыв удалён');
            refetchReviews();
        } catch (err) {
            showAlert('error', `❌ ${err.message}`);
        } finally {
            setDeletingReview(null);
        }
    };

    if (loading) return <Spinner text="Загружаем тайтл..." />;
    if (error) return (
        <div className="alert alert--error">
            ❌ {error}
            <button
                className="btn btn--ghost btn--sm"
                onClick={() => navigate(-1)}
                style={{ marginLeft: 12 }}
            >
                ← Назад
            </button>
        </div>
    );
    if (!comic) return null;

    const avgRating = reviews?.length > 0
        ? (reviews.reduce((s, r) => s + r.rating, 0) / reviews.length).toFixed(1)
        : null;

    const coverImage = images?.[0];
    const bannerImage = images?.[1] || images?.[0];
    const emoji = COVERS[comic.id % COVERS.length];

    return (
        <div className="ml-page">
            {/* Алерт */}
            {alert && (
                <div
                    className={`alert alert--${alert.type}`}
                    style={{
                        position: 'fixed',
                        top: 80,
                        right: 20,
                        zIndex: 999,
                        minWidth: 300,
                    }}
                >
                    {alert.text}
                </div>
            )}

            {/* ── БАННЕР ── */}
            <div className="ml-banner">
                {bannerImage ? (
                    <img
                        src={bannerImage.url}
                        alt=""
                        className="ml-banner__bg"
                    />
                ) : (
                    <div className="ml-banner__bg ml-banner__bg--empty" />
                )}
                <div className="ml-banner__overlay" />

                {/* Обложка */}
                <div
                    className="ml-cover"
                    onClick={() => images && images.length > 0 && setCoverLightbox(true)}
                >
                    {coverImage ? (
                        <img
                            src={coverImage.url}
                            alt={comic.title}
                            className="ml-cover__img"
                        />
                    ) : (
                        <div className="ml-cover__placeholder">
                            <span>{emoji}</span>
                        </div>
                    )}
                    {images && images.length > 1 && (
                        <div className="ml-cover__count">
                            🖼 {images.length}
                        </div>
                    )}
                </div>

                {/* Заголовок */}
                <div className="ml-banner__info">
                    <h1 className="ml-banner__title">{comic.title}</h1>
                    <div className="ml-banner__subtitle">
                        {comic.author?.name || ''}
                    </div>
                </div>

                {/* Рейтинг */}
                {avgRating && (
                    <div className="ml-banner__rating">
                        <span className="ml-banner__rating-star">★</span>
                        <span className="ml-banner__rating-value">{avgRating}</span>
                        <span className="ml-banner__rating-count">
                            {reviews.length} отз.
                        </span>
                    </div>
                )}
            </div>

            {/* ── ТЕЛО ── */}
            <div className="ml-body">

                {/* ── ЛЕВАЯ КОЛОНКА ── */}
                <div className="ml-sidebar">
                    <button
                        className="ml-read-btn"
                        onClick={() => setActiveTab('Главы')}
                    >
                        📖 Читать
                    </button>

                    <div className="ml-meta">
                        <div className="ml-meta__item">
                            <div className="ml-meta__label">Год выпуска</div>
                            <div className="ml-meta__value">{comic.releaseYear} г.</div>
                        </div>
                        {comic.author && (
                            <div className="ml-meta__item">
                                <div className="ml-meta__label">Автор</div>
                                <div className="ml-meta__value">{comic.author.name}</div>
                            </div>
                        )}
                        {comic.publisher && (
                            <div className="ml-meta__item">
                                <div className="ml-meta__label">Издатель</div>
                                <div className="ml-meta__value">{comic.publisher.name}</div>
                            </div>
                        )}
                        {comic.genres?.length > 0 && (
                            <div className="ml-meta__item">
                                <div className="ml-meta__label">Жанры</div>
                                <div
                                    className="ml-meta__value"
                                    style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}
                                >
                                    {comic.genres.map((g) => (
                                        <Badge key={g.id}>{g.name}</Badge>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                </div>

                {/* ── ПРАВАЯ ЧАСТЬ ── */}
                <div className="ml-content">

                    {/* Вкладки */}
                    <div className="ml-tabs">
                        {TABS.map((tab) => (
                            <button
                                key={tab}
                                className={`ml-tab${activeTab === tab ? ' ml-tab--active' : ''}`}
                                onClick={() => setActiveTab(tab)}
                            >
                                {tab}
                            </button>
                        ))}
                    </div>

                    {/* ── О ТАЙТЛЕ ── */}
                    {activeTab === 'О тайтле' && (
                        <div className="ml-tab-content">
                            <div className="ml-about">
                                <div className="ml-about__genres">
                                    {comic.genres?.map((g) => (
                                        <span key={g.id} className="ml-genre-tag">
                                            # {g.name}
                                        </span>
                                    ))}
                                </div>

                                <div className="ml-about__stats">
                                    <div className="ml-stat">
                                        <div className="ml-stat__value">
                                            {comic.releaseYear}
                                        </div>
                                        <div className="ml-stat__label">Год</div>
                                    </div>
                                    <div className="ml-stat">
                                        <div className="ml-stat__value">
                                            {reviews?.length ?? 0}
                                        </div>
                                        <div className="ml-stat__label">Отзывов</div>
                                    </div>
                                    <div className="ml-stat">
                                        <div className="ml-stat__value">
                                            {images?.length ?? 0}
                                        </div>
                                        <div className="ml-stat__label">Изображений</div>
                                    </div>
                                    {avgRating && (
                                        <div className="ml-stat">
                                            <div
                                                className="ml-stat__value"
                                                style={{ color: 'var(--warning)' }}
                                            >
                                                ★ {avgRating}
                                            </div>
                                            <div className="ml-stat__label">Рейтинг</div>
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    )}

                    {/* ── ГЛАВЫ ── */}
                    {activeTab === 'Главы' && (
                        <div className="ml-tab-content">
                            <ChapterList comicId={id} editMode={false} />
                        </div>
                    )}

                    {/* ── ОТЗЫВЫ ── */}
                    {activeTab === 'Отзывы' && (
                        <div className="ml-tab-content">
                            <div
                                style={{
                                    display: 'flex',
                                    justifyContent: 'space-between',
                                    alignItems: 'center',
                                    marginBottom: 20,
                                }}
                            >
                                <div
                                    className="ml-section-title"
                                    style={{ margin: 0 }}
                                >
                                    💬 Отзывы
                                    <span
                                        style={{
                                            fontSize: 13,
                                            fontWeight: 400,
                                            color: 'var(--text-muted)',
                                            marginLeft: 8,
                                        }}
                                    >
                                        {reviews?.length ?? 0}
                                    </span>
                                </div>
                                <button
                                    className="btn btn--primary btn--sm"
                                    onClick={() => setShowReviewModal(true)}
                                >
                                    📝 Написать отзыв
                                </button>
                            </div>

                            {!reviews || reviews.length === 0 ? (
                                <div className="empty-state" style={{ padding: '60px 0' }}>
                                    <div className="empty-state__icon">💬</div>
                                    <div className="empty-state__title">
                                        Отзывов пока нет
                                    </div>
                                    <p className="empty-state__text">Будьте первым!</p>
                                    <button
                                        className="btn btn--primary"
                                        style={{ marginTop: 16 }}
                                        onClick={() => setShowReviewModal(true)}
                                    >
                                        Написать отзыв
                                    </button>
                                </div>
                            ) : (
                                <div>
                                    {reviews.map((review) => (
                                        <div key={review.id} className="review-card">
                                            <div className="review-card__header">
                                                <div className="review-card__user">
                                                    <div className="review-card__avatar">
                                                        👤
                                                    </div>
                                                    <div className="review-card__name">
                                                        Читатель #{review.id}
                                                    </div>
                                                </div>
                                                <Rating value={review.rating} />
                                            </div>
                                            <div className="review-card__text">
                                                {review.text}
                                            </div>
                                            <div className="review-card__actions">
                                                <button
                                                    className="btn btn--danger btn--sm"
                                                    onClick={() =>
                                                        handleDeleteReview(review.id)
                                                    }
                                                    disabled={deletingReview === review.id}
                                                >
                                                    {deletingReview === review.id
                                                        ? '⏳'
                                                        : '🗑️'}{' '}
                                                    Удалить
                                                </button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}

                    {/* ── РЕДАКТИРОВАТЬ ── */}
                    {activeTab === 'Редактировать' && (
                        <div className="ml-tab-content">
                            <div className="ml-edit-grid">

                                {/* Основные данные */}
                                <div className="ml-edit-block">
                                    <div className="ml-section-title">
                                        📝 Основные данные
                                    </div>
                                    <ComicForm
                                        initial={comic}
                                        onSubmit={handleUpdate}
                                        onCancel={() => setActiveTab('О тайтле')}
                                        loading={saving}
                                    />
                                </div>

                                {/* Обложки */}
                                <div className="ml-edit-block">
                                    <div className="ml-section-title">
                                        🖼️ Обложки и изображения
                                    </div>
                                    <p
                                        style={{
                                            color: 'var(--text-muted)',
                                            fontSize: 13,
                                            marginBottom: 16,
                                            lineHeight: 1.6,
                                        }}
                                    >
                                        Первое изображение — обложка карточки.
                                        Второе — баннер вверху страницы.
                                    </p>
                                    <ImageGallery
                                        comicId={id}
                                        editable={true}
                                        onUpdate={refetchImages}
                                    />
                                </div>

                                {/* Главы */}
                                <div className="ml-edit-block ml-edit-block--full">
                                    <div className="ml-section-title">
                                        📖 Управление главами
                                    </div>
                                    <ChapterList comicId={id} editMode={true} />
                                </div>

                                {/* Опасная зона */}
                                <div className="ml-edit-block ml-edit-block--full ml-edit-block--danger">
                                    <div
                                        className="ml-section-title"
                                        style={{ color: 'var(--danger)' }}
                                    >
                                        ⚠️ Опасная зона
                                    </div>
                                    <p
                                        style={{
                                            color: 'var(--text-muted)',
                                            fontSize: 13,
                                            marginBottom: 16,
                                        }}
                                    >
                                        Удаление комикса необратимо. Все главы и
                                        изображения будут удалены с сервера.
                                    </p>
                                    <button
                                        className="btn btn--danger"
                                        onClick={() => setShowDeleteConfirm(true)}
                                    >
                                        🗑️ Удалить комикс навсегда
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            {/* Модалка отзыва */}
            {showReviewModal && (
                <Modal
                    title="📝 Написать отзыв"
                    onClose={() => setShowReviewModal(false)}
                >
                    <ReviewForm
                        comicId={id}
                        onSubmit={handleAddReview}
                        onCancel={() => setShowReviewModal(false)}
                        loading={saving}
                    />
                </Modal>
            )}

            {/* Подтверждение удаления */}
            {showDeleteConfirm && (
                <Modal
                    title="🗑️ Удалить комикс?"
                    onClose={() => setShowDeleteConfirm(false)}
                    footer={
                        <>
                            <button
                                className="btn btn--ghost"
                                onClick={() => setShowDeleteConfirm(false)}
                            >
                                Отмена
                            </button>
                            <button
                                className="btn btn--danger"
                                onClick={handleDelete}
                            >
                                Удалить навсегда
                            </button>
                        </>
                    }
                >
                    <p style={{ color: 'var(--text-secondary)' }}>
                        Вы уверены, что хотите удалить{' '}
                        <strong style={{ color: 'var(--text-primary)' }}>
                            «{comic.title}»
                        </strong>
                        ? Это действие необратимо.
                    </p>
                </Modal>
            )}
            {/* Лайтбокс обложек */}
            {coverLightbox && images && images.length > 0 && (
                <CoverLightbox
                    images={images}
                    startIdx={0}
                    onClose={() => setCoverLightbox(false)}
                />
            )}
        </div>
    );
}