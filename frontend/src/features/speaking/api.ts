import { useMutation } from '@tanstack/react-query'
import { toast } from 'sonner'
import api from '@/shared/api/client'
import type { ApiResponse, SpeakFeedback } from '@/shared/types/api'

export const useAssessFreeformSpeak = () =>
  useMutation({
    mutationFn: ({
      audio,
      situation,
      question,
      vocab,
      collocations,
    }: {
      audio: Blob
      situation: string
      question: string
      vocab: string[]
      collocations: string[]
    }) => {
      const form = new FormData()
      form.append('audio', audio, 'recording.webm')
      form.append('situation', situation)
      form.append('question', question)
      form.append('vocab', vocab.join(', '))
      form.append('collocations', collocations.join(', '))
      return api
        .post<never, ApiResponse<SpeakFeedback>>('/api/speak/freeform', form, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
        .then((r) => r.data)
    },
    onError: (err: Error) => toast.error(err.message),
  })
