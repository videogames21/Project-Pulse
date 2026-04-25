const BASE = 'http://localhost:8080'

async function request(method, path, body, params) {
  let url = `${BASE}${path}`
  if (params && Object.keys(params).length > 0) {
    url += '?' + new URLSearchParams(params).toString()
  }

  const token = localStorage.getItem('token')
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(url, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  const json = await res.json()

  if (!res.ok) {
    if (res.status === 401 && token && path !== '/api/v1/users/me/password') {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
      return
    }
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
  patch:  (path, body)   => request('PATCH',  path, body),
  delete: (path)         => request('DELETE', path),
}
