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

            {/* Верхняя панель */}
            <div
                className={`reader__topbar${showControls ? ' visible' : ''}`}
                onClick={(e) => e.stopPropagation()}
            >
                {/* Кнопка закрыть */}
                <button
                    onClick={(e) => { e.stopPropagation(); onClose(); }}
                    style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 8,
                        padding: '8px 18px',
                        borderRadius: '8px',
                        border: '1px solid rgba(255,255,255,0.15)',
                        background: 'rgba(255,255,255,0.08)',
                        color: '#fff',
                        fontSize: 14,
                        fontWeight: 600,
                        cursor: 'pointer',
                        backdropFilter: 'blur(8px)',
                        transition: 'all 0.2s',
                        letterSpacing: '0.3px',
                    }}
                    onMouseEnter={(e) => {
                        e.currentTarget.style.background = 'rgba(233,69,96,0.7)';
                        e.currentTarget.style.borderColor = 'rgba(233,69,96,0.8)';
                    }}
                    onMouseLeave={(e) => {
                        e.currentTarget.style.background = 'rgba(255,255,255,0.08)';
                        e.currentTarget.style.borderColor = 'rgba(255,255,255,0.15)';
                    }}
                >
                    <span style={{ fontSize: 16, lineHeight: 1 }}>✕</span>
                    Закрыть
                </button>

                {/* Название главы */}
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

            {/* Контент (Лента) */}
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

                {/* Навигация между главами */}
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