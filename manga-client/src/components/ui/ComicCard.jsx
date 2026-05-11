// manga-client/src/components/ui/ComicCard.jsx
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Badge from './Badge.jsx';
import { comicImagesApi, reviewsApi } from '../../api/api.js';

const COVERS = ['📚','📖','🗡️','⚔️','🌸','🔮','🦊','🐉','💫','🌙'];

export default function ComicCard({ comic }) {
    const navigate = useNavigate();
    const [coverUrl, setCoverUrl] = useState(null);
    const [avgRating, setAvgRating] = useState(null);

    useEffect(() => {
        comicImagesApi.getAll(comic.id)
            .then((imgs) => { if (imgs.length > 0) setCoverUrl(imgs[0].url); })
            .catch(() => {});
    }, [comic.id]);

    useEffect(() => {
        reviewsApi.getByComicId(comic.id)
            .then((reviews) => {
                if (reviews && reviews.length > 0) {
                    const avg = reviews.reduce((s, r) => s + r.rating, 0) / reviews.length;
                    setAvgRating(avg.toFixed(1));
                }
            })
            .catch(() => {});
    }, [comic.id]);

    return (
        <div className="comic-card" onClick={() => navigate(`/comics/${comic.id}`)}>
            <div className="comic-card__cover">
                {coverUrl ? (
                    <img
                        src={coverUrl}
                        alt={comic.title}
                        className="comic-card__cover-img"
                    />
                ) : (
                    <div className="comic-card__cover-placeholder">
                        <span>{COVERS[comic.id % COVERS.length]}</span>
                    </div>
                )}

                {/* ── Рейтинг — левый верхний угол ── */}
                {avgRating && (
                    <div className="comic-card__rating-badge">
                        ★ {avgRating}
                    </div>
                )}

                <div className="comic-card__year-badge">{comic.releaseYear}</div>
                <div className="comic-card__overlay">
                    <div className="comic-card__read-btn">Подробнее →</div>
                </div>
            </div>
            <div className="comic-card__body">
                <div className="comic-card__title">{comic.title}</div>
                <div className="comic-card__meta">
                    <span className="comic-card__author">
                        {comic.author?.name || 'Неизвестен'}
                    </span>
                </div>
                {comic.genres?.length > 0 && (
                    <div className="comic-card__genres">
                        {comic.genres.slice(0, 2).map((g) => (
                            <Badge key={g.id}>{g.name}</Badge>
                        ))}
                        {comic.genres.length > 2 && (
                            <Badge variant="accent">+{comic.genres.length - 2}</Badge>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}