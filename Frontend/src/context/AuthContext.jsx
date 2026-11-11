import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { getMe, loginUser, registerUser, UNAUTHORIZED_EVENT } from "../utils/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem("neurolearn_token") || null);
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem("neurolearn_user");
    return saved ? JSON.parse(saved) : null;
  });
  // Only "loading" while a stored token is being re-validated on startup.
  const [loading, setLoading] = useState(() => Boolean(localStorage.getItem("neurolearn_token")));

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
    localStorage.removeItem("neurolearn_token");
    localStorage.removeItem("neurolearn_user");
  }, []);

  useEffect(() => {
    if (!token) return;
    getMe()
      .then((userData) => {
        setUser(userData);
        localStorage.setItem("neurolearn_user", JSON.stringify(userData));
      })
      .catch(() => {
        // Token expired or invalid
        logout();
      })
      .finally(() => setLoading(false));
  }, [token, logout]);

  // api.js clears storage and fires this on any 401; mirror it in React state.
  useEffect(() => {
    const onUnauthorized = () => {
      setToken(null);
      setUser(null);
    };
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
  }, []);

  const handleLogin = async (email, password) => {
    const response = await loginUser({ email, password });
    setToken(response.token);
    setUser(response);
    localStorage.setItem("neurolearn_token", response.token);
    localStorage.setItem("neurolearn_user", JSON.stringify(response));
    return response;
  };

  const handleRegister = async (name, email, password, role) => {
    const response = await registerUser({ name, email, password, role });
    setToken(response.token);
    setUser(response);
    localStorage.setItem("neurolearn_token", response.token);
    localStorage.setItem("neurolearn_user", JSON.stringify(response));
    return response;
  };

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        role: user?.role || null,
        isAuthenticated: Boolean(token && user),
        loading,
        login: handleLogin,
        register: handleRegister,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

// Standard provider + hook pair; splitting them only matters for fast-refresh in dev.
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
