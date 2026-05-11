// manga-client/src/components/layout/Sidebar.jsx
import { NavLink } from 'react-router-dom';

const NAV_LINKS = [
    { to: '/', icon: '🏠', label: 'Главная' },
    { to: '/catalog', icon: '📚', label: 'Каталог' },
    { to: '/authors', icon: '✍️', label: 'Авторы' },
    { to: '/publishers', icon: '🏢', label: 'Издатели' },
    { to: '/genres', icon: '🏷️', label: 'Жанры' },
];

export default function Sidebar() {
    return (
        <aside className="sidebar">
            <div className="sidebar__section">
                <div className="sidebar__title">Навигация</div>
                {NAV_LINKS.map((link) => (
                    <NavLink
                        key={link.to}
                        to={link.to}
                        end={link.to === '/'}
                        className={({ isActive }) =>
                            `sidebar__link${isActive ? ' active' : ''}`
                        }
                    >
                        <span className="sidebar__link-icon">{link.icon}</span>
                        {link.label}
                    </NavLink>
                ))}
            </div>
        </aside>
    );
}