import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useForm } from 'react-hook-form';
import { LogIn, Key, AlertCircle } from 'lucide-react';

const Connexion = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  
  const {
    register,
    handleSubmit,
    formState: { errors }
  } = useForm();

  const onSubmit = async (data) => {
    setLoading(true);
    
    try {
      const result = await login(data.codeSession);
      
      if (result.success) {
        navigate('/test');
      }
    } catch (error) {
      console.error('Login error:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8 bg-gray-50">
      <div className="max-w-md w-full space-y-8">
        <div>
          <div className="mx-auto h-12 w-12 flex items-center justify-center rounded-full bg-blue-100">
            <Key className="h-6 w-6 text-blue-600" />
          </div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            Connexion à votre session
          </h2>
          <p className="mt-2 text-center text-sm text-gray-600">
            Entrez votre code de session pour accéder à votre test
          </p>
        </div>
        
        <div className="bg-white shadow-lg rounded-lg p-8">
          <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
            <div>
              <label htmlFor="codeSession" className="label">
                Code de session
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Key className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  id="codeSession"
                  type="text"
                  autoComplete="off"
                  className={`input pl-10 ${errors.codeSession ? 'border-red-500' : ''}`}
                  placeholder="Entrez votre code de session"
                  {...register('codeSession', { 
                    required: 'Le code de session est obligatoire',
                    minLength: {
                      value: 8,
                      message: 'Le code doit contenir au moins 8 caractères'
                    }
                  })}
                />
              </div>
              {errors.codeSession && (
                <p className="error-message">{errors.codeSession.message}</p>
              )}
            </div>

            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <div className="flex items-start">
                <AlertCircle className="h-5 w-5 text-blue-600 mt-0.5 mr-2 flex-shrink-0" />
                <div className="text-sm text-blue-800">
                  <p className="font-medium mb-1">Où trouver votre code de session ?</p>
                  <p> Votre code de session vous a été envoyé par email après votre inscription. 
                    Si vous ne l'avez pas reçu, vérifiez votre dossier spam ou contactez-nous.</p>
                </div>
              </div>
            </div>

            <div>
              <button
                type="submit"
                disabled={loading}
                className="group relative w-full flex justify-center py-3 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <span className="absolute left-0 inset-y-0 flex items-center pl-3">
                  <LogIn className="h-5 w-5 text-blue-500 group-hover:text-blue-400" />
                </span>
                {loading ? 'Connexion en cours...' : 'Se connecter'}
              </button>
            </div>

            <div className="text-center">
              <p className="text-sm text-gray-600">
                Pas encore de code de session ?{' '}
                <Link
                  to="/inscription"
                  className="font-medium text-blue-600 hover:text-blue-500"
                >
                  Inscrivez-vous
                </Link>
              </p>
            </div>
          </form>
        </div>

        <div className="text-center">
          <Link
            to="/"
            className="text-sm text-gray-500 hover:text-gray-700"
          >
            ← Retour à l'accueil
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Connexion;
