import { useState, useEffect } from 'react'

export function useCountUp(target: number, ms = 900) {
  const [n, setN] = useState(0)
  useEffect(() => {
    const steps = 40
    const inc = target / steps
    const delay = ms / steps
    let cur = 0
    const id = setInterval(() => {
      cur += inc
      if (cur >= target) {
        setN(target)
        clearInterval(id)
      } else {
        setN(Math.floor(cur))
      }
    }, delay)
    return () => clearInterval(id)
  }, [target, ms])
  return n
}
