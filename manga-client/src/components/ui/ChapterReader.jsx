// manga-client/src/components/ui/ChapterReader.jsx
import { useState, useEffect, useCallback, useRef, useMemo } from 'react';

export default function ChapterReader({ chapter, allChapters, onClose, onChapterChange }) {
    const [showControls, setShowControls] = useState(true);
    const scrollRef = useRef(null);

    const pages = chapter.pages || [];

    const logicalChapters = useMemo(() => {
        return [...allChapters].sort((a, b) => a.chapterNumber - b.chapterNumber);
    }, [allChapters]);

    const currentChapterIdx = logicalChapters.findIndex((c) => c.id === chapter.id);

    // Гарантированный скролл наверх при смене главы
    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = 0;
        }
    }, [chapter.id]);

    useEffect(() => {
        if (!showControls) return;
        const timer = setTimeout(() => setShowControls(false), 3000);
        return () => clearTimeout(timer);
    }, [showControls]);

    const goNextChapter = useCallback((e) => {
        if (e) e.stopPropagation();
        if (currentChapterIdx < logicalChapters.length - 1) {
            onChapterChange(logicalChapters[currentChapterIdx + 1]);
        }
    }, [currentChapterIdx, logicalChapters, onChapterChange]);

    const goPrevChapter = useCallback((e) => {
        if (e) e.stopPropagation();
        if (currentChapterIdx > 0) {
            onChapterChange(logicalChapters[currentChapterIdx - 1]);
        }
    }, [currentChapterIdx, logicalChapters, onChapterChange]);

    useEffect(() => {
        const handleKey = (e) => {
            if (e.key === 'Escape') onClose();
        };
        window.addEventListener('keydown', handleKey);
        return () => window.removeEventListener('keydown', handleKey);
    }, [onClose]);

    return (
        <div className="reader" onClick={() => setShowControls((v) => !v)}>
            <div
                className={`reader__topbar${showControls ? ' visible' : ''}`}
                onClick={(e) => e.stopPropagation()}
            >
                <button className="reader__btn" onClick={(e) => { e.stopPropagation(); onClose(); }}>
                    ✕ Закрыть
                </button>

                <div className="reader__chapter-info">
                    <span style={{ color: 'var(--accent)', fontWeight: 700 }}>
                        Глава {chapter.chapterNumber}
                    </span>
                    {chapter.title && (
                        <span style={{ color: 'var(--text-muted)', fontSize: 13 }}>
                            — {chapter.title}
                        </span>
                    )}
                </div>
                <div style={{ width: 100 }} />
            </div>

            <div className="reader__scroll" ref={scrollRef}>
                {pages.map((page, idx) => (
                    <div key={page.id} className="reader__scroll-page">
                        <div className="reader__page-num">Стр. {idx + 1}</div>
                        <img
                            src={page.url}
                            alt={`Страница ${idx + 1}`}
                            style={{ width: '100%', objectFit: 'contain' }}
                        />
                    </div>
                ))}

                <div className="reader__scroll-nav">
                    {currentChapterIdx > 0 && (
                        <button className="btn btn--secondary" onClick={goPrevChapter}>
                            ← Предыдущая глава
                        </button>
                    )}
                    {currentChapterIdx < logicalChapters.length - 1 && (
                        <button className="btn btn--primary" onClick={goNextChapter}>
                            Следующая глава →
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}