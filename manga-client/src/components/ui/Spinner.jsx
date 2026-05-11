export default function Spinner({ text = 'Загрузка...' }) {
    return (
        <div className="spinner-wrapper">
            <div className="spinner" />
            <span>{text}</span>
        </div>
    );
}