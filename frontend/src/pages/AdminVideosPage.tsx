import { useState } from 'react'
import { format, parseISO } from 'date-fns'
import {
  Upload, Play, Trash2, RefreshCw, ChevronLeft, ChevronRight,
  AlertCircle, CheckCircle2, Clock, Loader2, X,
} from 'lucide-react'
import PageHeader from '@/components/ui/PageHeader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import CefrBadge from '@/components/ui/CefrBadge'
import {
  useAdminVideos, useUploadVideo, useProcessVideo,
  useDeleteVideo, useAdminVideoStatus,
} from '@/features/videos/adminApi'
import type { Video, CefrLevel } from '@/shared/types/api'
import { cn } from '@/lib/utils'

const STATUS_CONFIG = {
  DRAFT:      { label: 'Draft',      cls: 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300' },
  PROCESSING: { label: 'Processing', cls: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300' },
  PUBLISHED:  { label: 'Published',  cls: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300' },
  FAILED:     { label: 'Failed',     cls: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300' },
}

const CEFR_OPTIONS: CefrLevel[] = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']
const TOPIC_OPTIONS = ['food', 'travel', 'technology', 'education', 'sport', 'health', 'business', 'science', 'culture', 'other']

// ── Upload dialog ─────────────────────────────────────────────────────────────

function UploadDialog({ onClose }: { onClose: () => void }) {
  const upload = useUploadVideo()
  const [file, setFile] = useState<File | null>(null)
  const [title, setTitle] = useState('')
  const [topic, setTopic] = useState('education')
  const [cefrLevel, setCefrLevel] = useState<CefrLevel>('B1')
  const [description, setDescription] = useState('')

  const handleSubmit = () => {
    if (!file || !title.trim()) return
    upload.mutate(
      { file, metadata: { title: title.trim(), topic, cefrLevel, description: description || undefined } },
      { onSuccess: onClose },
    )
  }

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div className="bg-background border rounded-xl w-full max-w-md shadow-xl">
        <div className="flex items-center justify-between px-5 py-4 border-b">
          <h2 className="font-semibold">Upload Video</h2>
          <button onClick={onClose} aria-label="Close"><X className="h-4 w-4" /></button>
        </div>

        <div className="p-5 space-y-4">
          {upload.isError && (
            <div className="flex items-start gap-2 rounded-lg bg-red-50 dark:bg-red-950/20 border border-red-200 dark:border-red-800 p-3 text-sm text-red-700 dark:text-red-400">
              <AlertCircle className="h-4 w-4 mt-0.5 shrink-0" />
              <span>{upload.error?.message ?? 'Upload failed'}</span>
            </div>
          )}
          {/* File picker */}
          <div>
            <Label>Video file (MP4)</Label>
            <div
              className={cn(
                'mt-1.5 border-2 border-dashed rounded-lg p-6 text-center cursor-pointer hover:border-primary transition-colors',
                file ? 'border-green-400 bg-green-50 dark:bg-green-950/10' : 'border-border',
              )}
              onClick={() => document.getElementById('video-file-input')?.click()}
            >
              {file ? (
                <div className="space-y-1">
                  <CheckCircle2 className="h-6 w-6 text-green-500 mx-auto" />
                  <p className="text-sm font-medium">{file.name}</p>
                  <p className="text-xs text-muted-foreground">{(file.size / 1024 / 1024).toFixed(1)} MB</p>
                </div>
              ) : (
                <div className="space-y-1 text-muted-foreground">
                  <Upload className="h-6 w-6 mx-auto opacity-50" />
                  <p className="text-sm">Click to select MP4 file</p>
                </div>
              )}
              <input
                id="video-file-input"
                type="file"
                accept="video/mp4,video/*"
                className="hidden"
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              />
            </div>
          </div>

          <div>
            <Label htmlFor="v-title">Title *</Label>
            <Input
              id="v-title"
              className="mt-1.5"
              placeholder="e.g. The Future of AI"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label>CEFR Level</Label>
              <select
                className="mt-1.5 w-full border rounded-md px-3 py-2 text-sm bg-background"
                value={cefrLevel}
                onChange={(e) => setCefrLevel(e.target.value as CefrLevel)}
              >
                {CEFR_OPTIONS.map((l) => <option key={l}>{l}</option>)}
              </select>
            </div>
            <div>
              <Label>Topic</Label>
              <select
                className="mt-1.5 w-full border rounded-md px-3 py-2 text-sm bg-background"
                value={topic}
                onChange={(e) => setTopic(e.target.value)}
              >
                {TOPIC_OPTIONS.map((t) => <option key={t}>{t}</option>)}
              </select>
            </div>
          </div>

          <div>
            <Label htmlFor="v-desc">Description (optional)</Label>
            <Input
              id="v-desc"
              className="mt-1.5"
              placeholder="Brief description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
        </div>

        <div className="px-5 py-4 border-t flex gap-3">
          <Button variant="outline" className="flex-1" onClick={onClose}>Cancel</Button>
          <Button
            className="flex-1 gap-2"
            onClick={handleSubmit}
            disabled={!file || !title.trim() || upload.isPending}
          >
            {upload.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
            Upload
          </Button>
        </div>
      </div>
    </div>
  )
}

// ── Video row ─────────────────────────────────────────────────────────────────

function VideoRow({ video }: { video: Video }) {
  const processVideo = useProcessVideo()
  const deleteVideo = useDeleteVideo()
  const isProcessing = video.status === 'PROCESSING'

  const { data: liveStatus } = useAdminVideoStatus(video.id, isProcessing)
  const displayStatus = liveStatus?.status ?? video.status

  const cfg = STATUS_CONFIG[displayStatus as keyof typeof STATUS_CONFIG] ?? STATUS_CONFIG.DRAFT

  return (
    <tr className="border-b last:border-0 hover:bg-muted/30 transition-colors">
      {/* Thumbnail */}
      <td className="px-4 py-3 w-20">
        {video.thumbnailUrl ? (
          <img src={video.thumbnailUrl} alt="" className="w-16 h-10 object-cover rounded" />
        ) : (
          <div className="w-16 h-10 rounded bg-muted flex items-center justify-center">
            <Play className="h-4 w-4 text-muted-foreground opacity-40" />
          </div>
        )}
      </td>

      {/* Info */}
      <td className="px-4 py-3 min-w-0">
        <p className="text-sm font-medium line-clamp-1">{video.title}</p>
        <div className="flex items-center gap-2 mt-0.5">
          <CefrBadge level={video.cefrLevel} />
          <span className="text-xs text-muted-foreground capitalize">{video.topic}</span>
          <span className="text-xs text-muted-foreground">{video.durationSec}s</span>
        </div>
      </td>

      {/* Status */}
      <td className="px-4 py-3 whitespace-nowrap">
        <span className={cn('text-xs px-2 py-1 rounded-full font-medium flex items-center gap-1 w-fit', cfg.cls)}>
          {displayStatus === 'PROCESSING' && <Loader2 className="h-3 w-3 animate-spin" />}
          {displayStatus === 'PUBLISHED' && <CheckCircle2 className="h-3 w-3" />}
          {displayStatus === 'FAILED' && <AlertCircle className="h-3 w-3" />}
          {displayStatus === 'DRAFT' && <Clock className="h-3 w-3" />}
          {cfg.label}
        </span>
        {(liveStatus?.errorMessage ?? video.errorMessage) && (
          <p className="text-xs text-red-500 mt-1 max-w-[200px] line-clamp-2" title={liveStatus?.errorMessage ?? video.errorMessage ?? ''}>
            {liveStatus?.errorMessage ?? video.errorMessage}
          </p>
        )}
      </td>

      {/* Date */}
      <td className="px-4 py-3 text-xs text-muted-foreground whitespace-nowrap hidden md:table-cell">
        {video.createdAt ? format(parseISO(video.createdAt as string), 'MMM d, yyyy') : '—'}
      </td>

      {/* Actions */}
      <td className="px-4 py-3 whitespace-nowrap">
        <div className="flex items-center gap-1.5">
          {(displayStatus === 'DRAFT' || displayStatus === 'FAILED') && (
            <Button
              size="sm"
              variant="outline"
              className="h-7 text-xs gap-1"
              onClick={() => processVideo.mutate(video.id)}
              disabled={processVideo.isPending}
            >
              <RefreshCw className="h-3 w-3" />
              Process
            </Button>
          )}
          <Button
            size="sm"
            variant="ghost"
            className="h-7 text-destructive hover:text-destructive hover:bg-destructive/10"
            onClick={() => {
              if (confirm(`Delete "${video.title}"?`)) deleteVideo.mutate(video.id)
            }}
            aria-label="Delete"
          >
            <Trash2 className="h-3.5 w-3.5" />
          </Button>
        </div>
      </td>
    </tr>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

export default function AdminVideosPage() {
  const [page, setPage] = useState(0)
  const [showUpload, setShowUpload] = useState(false)
  const { data, isLoading, refetch } = useAdminVideos(page)

  return (
    <div className="space-y-4">
      <PageHeader
        title="Admin — Videos"
        description="Upload and manage learning videos"
        action={
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => refetch()}>
              <RefreshCw className="h-4 w-4" />
            </Button>
            <Button size="sm" className="gap-2" onClick={() => setShowUpload(true)}>
              <Upload className="h-4 w-4" />
              Upload video
            </Button>
          </div>
        }
      />

      {/* Summary chips */}
      {data && (
        <div className="flex gap-2 flex-wrap">
          {(['DRAFT', 'PROCESSING', 'PUBLISHED', 'FAILED'] as const).map((s) => {
            const count = data.content.filter((v) => v.status === s).length
            if (!count) return null
            const cfg = STATUS_CONFIG[s]
            return (
              <span key={s} className={cn('text-xs px-2.5 py-1 rounded-full font-medium', cfg.cls)}>
                {cfg.label}: {count}
              </span>
            )
          })}
          <span className="text-xs text-muted-foreground px-2 py-1">
            Total: {data.totalElements}
          </span>
        </div>
      )}

      {/* Table */}
      <div className="border rounded-xl overflow-hidden">
        {isLoading ? (
          <div className="p-8 flex justify-center">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        ) : !data || data.content.length === 0 ? (
          <div className="flex flex-col items-center py-16 gap-3 text-muted-foreground">
            <Play className="h-10 w-10 opacity-20" />
            <p className="text-sm">No videos yet</p>
            <Button size="sm" onClick={() => setShowUpload(true)}>Upload first video</Button>
          </div>
        ) : (
          <table className="w-full">
            <thead>
              <tr className="border-b bg-muted/30 text-xs text-muted-foreground uppercase tracking-wide">
                <th className="px-4 py-2.5 text-left">Thumbnail</th>
                <th className="px-4 py-2.5 text-left">Title</th>
                <th className="px-4 py-2.5 text-left">Status</th>
                <th className="px-4 py-2.5 text-left hidden md:table-cell">Uploaded</th>
                <th className="px-4 py-2.5 text-left">Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((v) => <VideoRow key={v.id} video={v} />)}
            </tbody>
          </table>
        )}
      </div>

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-3">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <span className="text-sm text-muted-foreground">{page + 1} / {data.totalPages}</span>
          <Button variant="outline" size="sm" disabled={data.last} onClick={() => setPage((p) => p + 1)}>
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      )}

      {showUpload && <UploadDialog onClose={() => setShowUpload(false)} />}
    </div>
  )
}
