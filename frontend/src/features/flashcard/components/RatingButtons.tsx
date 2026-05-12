const RATINGS = [
  { value: 1 as const, label: 'Again', hint: '<1 min', className: 'border-red-300 text-red-600 hover:bg-red-50 hover:border-red-400' },
  { value: 2 as const, label: 'Hard',  hint: '~1 day', className: 'border-orange-300 text-orange-600 hover:bg-orange-50 hover:border-orange-400' },
  { value: 3 as const, label: 'Good',  hint: '~3 days', className: 'border-green-300 text-green-600 hover:bg-green-50 hover:border-green-400' },
  { value: 4 as const, label: 'Easy',  hint: '~1 week', className: 'border-blue-300 text-blue-600 hover:bg-blue-50 hover:border-blue-400' },
]

interface RatingButtonsProps {
  onRate: (rating: 1 | 2 | 3 | 4) => void
  disabled?: boolean
}

export default function RatingButtons({ onRate, disabled }: RatingButtonsProps) {
  return (
    <div className="flex gap-3 justify-center flex-wrap">
      {RATINGS.map(({ value, label, hint, className }) => (
        <button
          key={value}
          onClick={() => onRate(value)}
          disabled={disabled}
          className={`flex flex-col items-center gap-0.5 px-5 py-3 rounded-lg border-2 bg-background transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${className}`}
        >
          <span className="text-sm font-semibold">{label}</span>
          <span className="text-xs opacity-70">{hint}</span>
          <span className="text-[10px] opacity-50 mt-0.5">Press {value}</span>
        </button>
      ))}
    </div>
  )
}
