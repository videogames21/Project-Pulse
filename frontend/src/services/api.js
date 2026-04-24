const BASE = 'http://localhost:8080'

async function request(method, path, body, params) {
  let url = `${BASE}${path}`
  if (params && Object.keys(params).length > 0) {
    url += '?' + new URLSearchParams(params).toString()
  }

  const res = await fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  })

  const json = await res.json()

  if (!res.ok) {
    const err = new Error(json.message ?? `${method} ${path} → ${res.status}`)
    err.status = res.status
    err.data = json
    throw err
  }

  return json
}

export const api = {
  get:    (path, params) => request('GET',    path, null, params),
  post:   (path, body)   => request('POST',   path, body),
  put:    (path, body)   => request('PUT',    path, body),
  delete: (path)         => request('DELETE', path),
}
