export default function Footer() {
    return (
        <footer
            style={{
                borderTop: '1px solid var(--border)',
                padding: '24px 28px',
                color: 'var(--text-muted)',
                fontSize: '13px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: '12px',
            }}
        >
            <span>© 2024 NEMangaLib — Каталог манги</span>
            <span>Читайте любимую мангу онлайн</span>
        </footer>
    );
}