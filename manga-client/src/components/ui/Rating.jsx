export default function Rating({ value, count }) {
    return (
        <div className="rating">
            <span className="rating__star">★</span>
            <span className="rating__value">{value}</span>
            {count !== undefined && (
                <span className="rating__count">({count})</span>
            )}
        </div>
    );
}