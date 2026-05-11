// manga-client/src/pages/HomePage.jsx
import { useNavigate } from 'react-router-dom';
import { useApi } from '../hooks/useApi.js';
import { comicsApi, authorsApi, genresApi } from '../api/api.js';
import ComicCard from '../components/ui/ComicCard.jsx';
import Spinner from '../components/ui/Spinner.jsx';

export default function HomePage() {
    const navigate = useNavigate();
    const { data: comics, loading: comicsLoading } = useApi(comicsApi.getAll);
    const { data: authors } = useApi(authorsApi.getAll);
    const { data: genres } = useApi(genresApi.getAll);

    // Сортируем по убыванию ID (чтобы первыми шли самые новые), затем берем 6 штук
    const recent = comics
        ? [...comics].sort((a, b) => b.id - a.id).slice(0, 6)
        : [];

    return (
        <div>
            {/* Hero */}
            <div className="hero">
                <div className="hero__subtitle">🔥 Не лучшая платформа для чтения</div>
                <h1 className="hero__title">
                    Читайте <span>мангу</span>,<br />и т.д.
                </h1>
                <p className="hero__description">
                    (не) Огромная коллекция тайтлов на русском языке. (не) Удобный каталог,
                    отзывы и рейтинги.
                </p>
                <div className="hero__stats">
                    <div className="hero__stat">
                        <div className="hero__stat-value">
                            {comics?.length ?? '...'}
                        </div>
                        <div className="hero__stat-label">Тайтлов</div>
                    </div>
                    <div className="hero__stat">
                        <div className="hero__stat-value">
                            {authors?.length ?? '...'}
                        </div>
                        <div className="hero__stat-label">Авторов</div>
                    </div>
                    <div className="hero__stat">
                        <div className="hero__stat-value">
                            {genres?.length ?? '...'}
                        </div>
                        <div className="hero__stat-label">Жанров</div>
                    </div>
                </div>
            </div>

            {/* Новинки */}
            <div className="section-header">
                <h2 className="section-title">🆕 Новые поступления</h2>
                <button
                    className="btn btn--ghost btn--sm"
                    onClick={() => navigate('/catalog')}
                >
                    Весь каталог →
                </button>
            </div>

            {comicsLoading ? (
                <Spinner />
            ) : recent.length > 0 ? (
                <div className="comic-grid">
                    {recent.map((comic) => (
                        <ComicCard key={comic.id} comic={comic} />
                    ))}
                </div>
            ) : (
                <div className="empty-state">
                    <div className="empty-state__icon">📭</div>
                    <div className="empty-state__title">Каталог пуст</div>
                    <p className="empty-state__text">
                        Начните добавлять комиксы через каталог
                    </p>
                </div>
            )}

            {/* Популярные жанры */}
            {genres && genres.length > 0 && (
                <>
                    <div className="section-header" style={{ marginTop: 40 }}>
                        <h2 className="section-title">🏷️ Популярные жанры</h2>
                    </div>
                    <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', paddingBottom: 40 }}>
                        {genres.map((g) => (
                            <button
                                key={g.id}
                                className="btn btn--secondary btn--sm"
                                onClick={() => navigate(`/catalog?genre=${encodeURIComponent(g.name)}`)}
                                style={{ background: 'var(--bg-secondary)', border: 'none' }}
                            >
                                {g.name}
                            </button>
                        ))}
                    </div>
                </>
            )}
        </div>
    );
}