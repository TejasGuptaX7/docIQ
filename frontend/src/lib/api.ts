// src/lib/api.ts

export const API_BASE = import.meta.env.VITE_API_URL!

export interface SearchPayload {
  query: string
  docId?: string
}

export interface SearchResult {
  answer: string
  sources: { page: number; excerpt: string; confidence: number }[]
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const text = await res.text()
    throw new Error(`API error ${res.status}: ${text}`)
  }
  return res.json()
}

/**
 * Calls POST /search with your query and optional docId.
 * Automatically includes Authorization header if a token is provided.
 */
export async function searchAPI(
  payload: SearchPayload,
  token?: string
): Promise<SearchResult> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const res = await fetch(`${API_BASE}/search`, {
    method: 'POST',
    headers,
    body: JSON.stringify(payload),
  })
  return handleResponse<SearchResult>(res)
}
