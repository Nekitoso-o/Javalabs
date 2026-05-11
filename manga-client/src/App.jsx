import { Routes, Route } from 'react-router-dom';
import Header from './components/layout/Header.jsx';
import Sidebar from './components/layout/Sidebar.jsx';
import Footer from './components/layout/Footer.jsx';
import HomePage from './pages/HomePage.jsx';
import CatalogPage from './pages/CatalogPage.jsx';
import ComicDetailPage from './pages/ComicDetailPage.jsx';
import AuthorsPage from './pages/AuthorsPage.jsx';
import PublishersPage from './pages/PublishersPage.jsx';
import GenresPage from './pages/GenresPage.jsx';

export default function App() {
    return (
        <>
            <Header />
            <div className="layout">
                <div className="layout__sidebar">
                    <Sidebar />
                </div>
                <main className="layout__main">
                    <div className="layout__content">
                        <Routes>
                            <Route path="/" element={<HomePage />} />
                            <Route path="/catalog" element={<CatalogPage />} />
                            <Route path="/comics/:id" element={<ComicDetailPage />} />
                            <Route path="/authors" element={<AuthorsPage />} />
                            <Route path="/publishers" element={<PublishersPage />} />
                            <Route path="/genres" element={<GenresPage />} />
                            <Route
                                path="*"
                                element={
                                    <div className="empty-state">
                                        <div className="empty-state__icon">🗺️</div>
                                        <div className="empty-state__title">404 — Страница не найдена</div>
                                        <a href="/manga-client/public" className="btn btn--primary" style={{ marginTop: 20 }}>
                                            На главную
                                        </a>
                                    </div>
                                }
                            />
                        </Routes>
                    </div>
                    <Footer />
                </main>
            </div>
        </>
    );
}