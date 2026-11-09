import { createContext, useContext, useEffect, useState } from "react";
import { getMe, loginUser, registerUser, UNAUTHORIZED_EVENT } from "../utils/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem("neurolearn_token") || null);
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem("neurolearn_user");
    return saved ? JSON.parse(saved) : null;
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (token) {
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
    } else {
      setLoading(false);
    }
  }, [token]);

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

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem("neurolearn_token");
    localStorage.removeItem("neurolearn_user");
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

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
