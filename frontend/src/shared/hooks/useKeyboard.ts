import { useEffect } from 'react'

type KeyHandler = (e: KeyboardEvent) => void

export function useKeyboard(handlers: Record<string, KeyHandler>) {
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (
        e.target instanceof HTMLInputElement ||
        e.target instanceof HTMLTextAreaElement
      ) return
      const handler = handlers[e.key] ?? handlers[e.code]
      if (handler) {
        e.preventDefault()
        handler(e)
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [handlers])
}
