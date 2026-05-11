import { useEffect, useState } from 'react';

export default function CoverLightbox({ images, startIdx = 0, onClose }) {
    const [idx, setIdx] = useState(startIdx);

    useEffect(() => {
        const handleKey = (e) => {
            if (e.key === 'Escape') onClose();
            if (e.key === 'ArrowRight') setIdx((i) => (i + 1) % images.length);
            if (e.key === 'ArrowLeft') setIdx((i) => (i - 1 + images.length) % images.length);
        };
        window.addEventListener('keydown', handleKey);
        // Запретить прокрутку body
        document.body.style.overflow = 'hidden';
        return () => {
            window.removeEventListener('keydown', handleKey);
            document.body.style.overflow = '';
        };
    }, [images.length, onClose]);

    if (!images || images.length === 0) return null;
    const current = images[idx];

    return (
        <div className="cover-lightbox" onClick={onClose}>
            <button
                className="cover-lightbox__close"
                onClick={(e) => { e.stopPropagation(); onClose(); }}
            >
                ✕
            </button>

            {/* Стрелки */}
            {images.length > 1 && (
                <>
                    <button
                        className="cover-lightbox__nav cover-lightbox__nav--prev"
                        onClick={(e) => {
                            e.stopPropagation();
                            setIdx((i) => (i - 1 + images.length) % images.length);
                        }}
                    >‹</button>
                    <button
                        className="cover-lightbox__nav cover-lightbox__nav--next"
                        onClick={(e) => {
                            e.stopPropagation();
                            setIdx((i) => (i + 1) % images.length);
                        }}
                    >›</button>
                </>
            )}

            {/* Главное изображение */}
            <div
                className="cover-lightbox__main"
                onClick={(e) => e.stopPropagation()}
            >
                <img
                    src={current.url}
                    alt={current.originalName}
                    className="cover-lightbox__img"
                />
            </div>

            {/* Миниатюры внизу */}
            {images.length > 1 && (
                <div
                    className="cover-lightbox__thumbs"
                    onClick={(e) => e.stopPropagation()}
                >
                    {images.map((img, i) => (
                        <div
                            key={img.id}
                            className={`cover-lightbox__thumb${i === idx ? ' active' : ''}`}
                            onClick={() => setIdx(i)}
                        >
                            <img src={img.url} alt="" />
                            <span className="cover-lightbox__thumb-num">{i + 1}</span>
                        </div>
                    ))}
                </div>
            )}

            {/* Счётчик */}
            <div className="cover-lightbox__counter">
                {idx + 1} / {images.length}
            </div>
        </div>
    );
}