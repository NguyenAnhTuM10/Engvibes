interface Props {
  url: string
  onProgress?: (playedSeconds: number) => void
}

export default function VideoPlayer({ url, onProgress }: Props) {
  if (!url) {
    return (
      <div className="aspect-video bg-muted rounded-lg flex items-center justify-center text-sm text-muted-foreground">
        Video not available
      </div>
    )
  }

  return (
    <div className="aspect-video rounded-lg overflow-hidden bg-black">
      <video
        src={url}
        controls
        className="w-full h-full"
        onTimeUpdate={(e) => onProgress?.(e.currentTarget.currentTime)}
      />
    </div>
  )
}
