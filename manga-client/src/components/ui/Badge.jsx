export default function Badge({ children, variant = 'genre' }) {
    return <span className={`badge badge--${variant}`}>{children}</span>;
}