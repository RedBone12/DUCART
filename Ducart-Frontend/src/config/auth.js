export function saveAuth(data) {
  localStorage.setItem("token", data.token || "");
  localStorage.setItem("userid", data.userid || "");
  localStorage.setItem("username", data.username || "");
  localStorage.setItem("name", data.name || "");
  localStorage.setItem("role", data.role || "");
}

export function clearAuth() {
  localStorage.removeItem("token");
  localStorage.removeItem("userid");
  localStorage.removeItem("username");
  localStorage.removeItem("name");
  localStorage.removeItem("role");
}

export function getToken() {
  return localStorage.getItem("token");
}

export function getRole() {
  return localStorage.getItem("role");
}

export function isLoggedIn() {
  return !!localStorage.getItem("token");
}

export function authJsonHeaders() {
  const token = getToken();
  const headers = {
    "content-type": "application/json",
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  return headers;
}

export function authOnlyHeaders() {
  const token = getToken();
  const headers = {};

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  return headers;
}
