import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useListPings, useCreatePing } from '@/features/demo/api'
import { Link } from 'react-router-dom'

const schema = z.object({
  message: z.string().min(1).max(200),
})
type FormData = z.infer<typeof schema>

export default function DemoPage() {
  const { data, isLoading } = useListPings()
  const { mutate, isPending } = useCreatePing()
  const { register, handleSubmit, reset, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const onSubmit = (values: FormData) => {
    mutate(values.message, { onSuccess: () => reset() })
  }

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="max-w-lg mx-auto">
        <div className="mb-4">
          <Link to="/" className="text-sm text-blue-600 hover:underline">← Back to Home</Link>
        </div>

        <h1 className="text-2xl font-bold mb-6">Demo — FE ↔ BE ↔ DB</h1>

        <form onSubmit={handleSubmit(onSubmit)} className="bg-white rounded-xl shadow p-6 mb-6">
          <label className="block text-sm font-medium text-gray-700 mb-1">Message</label>
          <input
            {...register('message')}
            className="w-full border rounded-lg px-3 py-2 text-sm mb-1 focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="Type a ping message..."
          />
          {errors.message && (
            <p className="text-red-500 text-xs mb-2">{errors.message.message}</p>
          )}
          <button
            type="submit"
            disabled={isPending}
            className="mt-2 w-full bg-blue-600 text-white rounded-lg py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
          >
            {isPending ? 'Sending...' : 'Send Ping'}
          </button>
        </form>

        <div className="bg-white rounded-xl shadow p-6">
          <h2 className="font-semibold mb-3">Pings from DB ({data?.data?.length ?? 0})</h2>
          {isLoading && <p className="text-gray-400 text-sm">Loading...</p>}
          {data?.data?.length === 0 && <p className="text-gray-400 text-sm">No pings yet.</p>}
          <ul className="space-y-2">
            {data?.data?.map((ping) => (
              <li key={ping.id} className="flex justify-between text-sm border-b pb-2">
                <span>{ping.message}</span>
                <span className="text-gray-400 text-xs">
                  {new Date(ping.createdAt).toLocaleTimeString()}
                </span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}
