import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import PageHeader from '@/components/ui/PageHeader'
import VideoCard from '@/features/videos/components/VideoCard'
import VideoFilters from '@/features/videos/components/VideoFilters'
import { useVideos, useRecommendedVideos } from '@/features/videos/api'
import type { CefrLevel, VideoFilter } from '@/shared/types/api'

export default function VideosPage() {
  const [searchParams, setSearchParams] = useSearchParams()

  const cefrLevel = (searchParams.get('cefrLevel') as CefrLevel | null) ?? null
  const topic = searchParams.get('topic')
  const searchParam = searchParams.get('search') ?? ''
  const page = parseInt(searchParams.get('page') ?? '0', 10)
  const [searchInput, setSearchInput] = useState(searchParam)

  const filter: VideoFilter = {
    ...(cefrLevel ? { cefrLevel } : {}),
    ...(topic ? { topic } : {}),
    ...(searchInput ? { search: searchInput } : {}),
    page,
    size: 12,
  }

  const { data: videosPage, isLoading } = useVideos(filter)
  const { data: recommended } = useRecommendedVideos(6)

  const setParam = (key: string, value: string | null) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev)
      if (value) next.set(key, value)
      else next.delete(key)
      next.delete('page')
      return next
    })
  }

  return (
    <div>
      <PageHeader title="Videos" description="Browse and learn from video lessons" />

      <VideoFilters
        search={searchInput}
        onSearchChange={(v) => {
          setSearchInput(v)
          setParam('search', v || null)
        }}
        cefrLevel={cefrLevel}
        onCefrChange={(v) => setParam('cefrLevel', v)}
        topic={topic}
        onTopicChange={(v) => setParam('topic', v)}
      />

      {recommended && recommended.length > 0 && (
        <section className="mb-8">
          <h2 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
            Recommended for you
          </h2>
          <div className="flex gap-4 overflow-x-auto pb-2 -mx-1 px-1">
            {recommended.map((v) => (
              <div key={v.id} className="min-w-[200px] max-w-[200px]">
                <VideoCard video={v} />
              </div>
            ))}
          </div>
        </section>
      )}

      <section>
        <h2 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
          All Videos
        </h2>

        {isLoading ? (
          <VideoGridSkeleton />
        ) : !videosPage || videosPage.content.length === 0 ? (
          <div className="flex flex-col items-center py-24 text-muted-foreground gap-2">
            <p className="text-base font-medium">No videos found</p>
            <p className="text-sm">Try adjusting the filters above</p>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {videosPage.content.map((v) => (
                <VideoCard key={v.id} video={v} />
              ))}
            </div>

            {videosPage.totalPages > 1 && (
              <div className="flex justify-center items-center gap-3 mt-6">
                <button
                  className="px-3 py-1.5 rounded border text-sm transition-colors hover:bg-accent disabled:opacity-40"
                  disabled={page === 0}
                  onClick={() => setParam('page', String(page - 1))}
                >
                  Previous
                </button>
                <span className="text-sm text-muted-foreground">
                  {page + 1} / {videosPage.totalPages}
                </span>
                <button
                  className="px-3 py-1.5 rounded border text-sm transition-colors hover:bg-accent disabled:opacity-40"
                  disabled={videosPage.last}
                  onClick={() => setParam('page', String(page + 1))}
                >
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  )
}

function VideoGridSkeleton() {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {Array.from({ length: 6 }).map((_, i) => (
        <div key={i} className="rounded-lg border bg-card overflow-hidden animate-pulse">
          <div className="aspect-video bg-muted" />
          <div className="p-3 space-y-2">
            <div className="h-4 bg-muted rounded w-3/4" />
            <div className="h-3 bg-muted rounded w-1/2" />
          </div>
        </div>
      ))}
    </div>
  )
}
