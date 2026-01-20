import React, { createContext, useContext, useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import API_BASE_URL from '../config/api';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const userData = localStorage.getItem('user');
    
    if (token && userData) {
      try {
        const parsedUser = JSON.parse(userData);
        setUser(parsedUser);
      } catch (error) {
        console.error('Error parsing user data:', error);
        localStorage.removeItem('token');
        localStorage.removeItem('user');
      }
    }
    setLoading(false);
  }, []);

  const login = async (codeSession) => {
    try {
      const response = await fetch(`${API_BASE_URL}/candidats/connexion`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ codeSession }),
      });

      const contentType = response.headers.get('content-type') || '';
      let data = {};
      if (contentType.includes('application/json')) {
        data = await response.json();
      } else {
        const text = await response.text();
        console.error('Backend returned non-JSON response:', text);
        data = { error: text };
      }

      if (response.ok) {
        setUser(data.candidat);
        localStorage.setItem('user', JSON.stringify(data.candidat));
        toast.success('Connexion réussie!');
        return { success: true };
      } else {
        const errorMessage = data.error || `Erreur de connexion (${response.status})`;
        console.error('Login failed:', { status: response.status, data, errorMessage });
        toast.error(errorMessage);
        return { success: false, error: errorMessage };
      }
    } catch (error) {
      console.error('Login error:', error);
      toast.error('Erreur de connexion au serveur');
      return { success: false, error: 'Erreur de connexion au serveur' };
    }
  };

  const loginAdmin = async (username, password) => {
    try {
      const response = await fetch(`${API_BASE_URL}/admin/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password }),
      });

      const data = await response.json();

      if (response.ok) {
        const adminUser = { ...data.admin, role: 'admin' };
        setUser(adminUser);
        localStorage.setItem('user', JSON.stringify(adminUser));
        toast.success('Connexion admin réussie!');
        return { success: true };
      } else {
        toast.error(data.error || 'Erreur de connexion admin');
        return { success: false, error: data.error };
      }
    } catch (error) {
      console.error('Admin login error:', error);
      toast.error('Erreur de connexion au serveur');
      return { success: false, error: 'Erreur de connexion au serveur' };
    }
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    toast.success('Déconnexion réussie');
  };

  const value = {
    user,
    login,
    loginAdmin,
    logout,
    loading,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
