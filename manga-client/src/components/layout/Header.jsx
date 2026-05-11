// manga-client/src/components/layout/Header.jsx
import { useState } from 'react';
import { useNavigate, NavLink, useLocation } from 'react-router-dom';

export default function Header() {
    const [query, setQuery] = useState('');
    const navigate = useNavigate();
    const location = useLocation();

    const isCatalog = location.pathname === '/catalog';

    const handleSearch = (e) => {
        e.preventDefault();
        if (query.trim()) {
            navigate(`/catalog?search=${encodeURIComponent(query.trim())}`);
            setQuery('');
        }
    };

    return (
        <header className="header">
            <NavLink to="/" className="header__logo">
                <img
                    src="/logo.png"
                    alt="MangaLib"
                    style={{ width: 36, height: 36, objectFit: 'contain', borderRadius: 8 }}
                />
                NeManga<span>Lib</span>
            </NavLink>

            {!isCatalog && (
                <form className="header__search" onSubmit={handleSearch}>
                    <span className="header__search-icon">🔍</span>
                    <input
                        className="header__search-input"
                        placeholder="Поиск..."
                        value={query}
                        onChange={(e) => setQuery(e.target.value)}
                    />
                </form>
            )}
        </header>
    );
}