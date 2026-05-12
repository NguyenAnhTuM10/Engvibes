import { memo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Play } from 'lucide-react'
import CefrBadge from '@/components/ui/CefrBadge'
import type { Video } from '@/shared/types/api'

function fmt(sec: number) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

const VideoCard = memo(function VideoCard({ video }: { video: Video }) {
  const navigate = useNavigate()

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={() => navigate(`/session/${video.id}`)}
      onKeyDown={(e) => e.key === 'Enter' && navigate(`/session/${video.id}`)}
      className="group cursor-pointer rounded-lg overflow-hidden border bg-card hover:shadow-md transition-all duration-200 hover:scale-[1.02] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      <div className="relative aspect-video bg-muted">
        {video.thumbnailUrl ? (
          <img
            src={video.thumbnailUrl}
            alt={video.title}
            loading="lazy"
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            <Play className="h-10 w-10 text-muted-foreground opacity-30" />
          </div>
        )}
        <span className="absolute bottom-1.5 right-1.5 bg-black/70 text-white text-xs px-1.5 py-0.5 rounded font-mono">
          {fmt(video.durationSec)}
        </span>
      </div>

      <div className="p-3">
        <p className="font-medium text-sm line-clamp-2 mb-2 leading-snug">{video.title}</p>
        <div className="flex items-center gap-2 flex-wrap">
          <CefrBadge level={video.cefrLevel} />
          {video.topic && (
            <span className="text-xs bg-secondary text-secondary-foreground px-2 py-0.5 rounded-full capitalize">
              {video.topic}
            </span>
          )}
        </div>
      </div>
    </div>
  )
})

export default VideoCard
