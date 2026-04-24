const BASE = 'http://localhost:8080'

async function request(method, path, body, params) {
  // 1. Build the URL with query parameters (from Snippet 2)
  let url = `${BASE}${path}`;
  if (params && Object.keys(params).length > 0) {
    url += '?' + new URLSearchParams(params).toString();
  }

  // 2. Build the Headers with the Authorization token (from Snippet 1)
  const token = localStorage.getItem('token');
  const headers = { 'Content-Type': 'application/json' };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // 3. Execute the fetch request using the dynamic URL and Headers
  const res = await fetch(url, {
    method,
    headers, // <-- Fixed: Pass the built headers object here instead of hardcoding it
    body: body ? JSON.stringify(body) : undefined,
  });

  // return res; (Assuming your function finishes out down here)
}

  const json = await res.json()

  if (!res.ok) {
    if (res.status === 401 && token && path !== '/api/v1/users/me/password') {
      // Session expired — clear stored session and redirect to login
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
  post:   (path, body)  => request('POST',   path, body),
  put:    (path, body)  => request('PUT',    path, body),
  patch:  (path, body)  => request('PATCH',  path, body),
  delete: (path)        => request('DELETE', path),
}
