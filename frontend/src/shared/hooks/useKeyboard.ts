import { useEffect, useRef } from 'react'

type KeyHandler = (e: KeyboardEvent) => void

export function useKeyboard(handlers: Record<string, KeyHandler>) {
  const ref = useRef(handlers)
  ref.current = handlers  // always latest, no stale closure

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (
        e.target instanceof HTMLInputElement ||
        e.target instanceof HTMLTextAreaElement
      ) return
      const handler = ref.current[e.key] ?? ref.current[e.code]
      if (handler) {
        e.preventDefault()
        handler(e)
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, []) // mount/unmount only — handlers always fresh via ref
}
