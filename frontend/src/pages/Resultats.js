import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import API_BASE_URL from '../config/api';
import { 
  BarChart3, 
  Trophy, 
  Clock, 
  CheckCircle, 
  XCircle, 
  BookOpen,
  TrendingUp,
  Calendar,
  Award
} from 'lucide-react';

const Resultats = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [sessions, setSessions] = useState([]);
  const [selectedSession, setSelectedSession] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchSessions = useCallback(async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/resultats/candidat/${user.id}`);
      if (response.ok) {
        const data = await response.json();
        setSessions(data.sessions || []); 
        if (data.sessions && data.sessions.length > 0) {
          setSelectedSession(data.sessions[0]);
        }
      } else {
        const errorData = await response.json();
        toast.error(errorData.error || 'Erreur lors du chargement des résultats');
      }
    } catch (error) {
      console.error('Error fetching sessions:', error);
      toast.error('Erreur de connexion au serveur');
    } finally {
      setLoading(false);
    }
  }, [user.id]);

  useEffect(() => {
    if (!user) {
      navigate('/connexion');
      return;
    }
    // Forcer le rechargement des résultats à chaque visite de la page
    fetchSessions();
  }, [user, navigate, fetchSessions]);

  // Ajouter un écouteur d'événements pour recharger les résultats
  useEffect(() => {
    const handleRefresh = () => {
      fetchSessions();
    };

    // Écouter les événements de rafraîchissement personnalisés
    window.addEventListener('refreshResults', handleRefresh);
    
    // Rafraîchir automatiquement après 2 secondes pour être sûr
    const autoRefresh = setTimeout(() => {
      fetchSessions();
    }, 2000);
    
    return () => {
      window.removeEventListener('refreshResults', handleRefresh);
      clearTimeout(autoRefresh);
    };
  }, [fetchSessions]);

  const fetchSessionDetails = async (sessionId) => {
    try {
      const response = await fetch(`${API_BASE_URL}/resultats/session/${sessionId}`);
      if (response.ok) {
        const data = await response.json();
        setSelectedSession(data);
      }
    } catch (error) {
      console.error('Error fetching session details:', error);
      toast.error('Erreur lors du chargement des détails');
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getScoreColor = (percentage) => {
    if (percentage >= 80) return 'text-green-600';
    if (percentage >= 60) return 'text-yellow-600';
    return 'text-red-600';
  };

  const getScoreBgColor = (percentage) => {
    if (percentage >= 80) return 'bg-green-100';
    if (percentage >= 60) return 'bg-yellow-100';
    return 'bg-red-100';
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="loading"></div>
      </div>
    );
  }

  if (!user) {
    return null;
  }

  return (
    <div className="min-h-screen py-8">
      <div className="container mx-auto px-4 max-w-6xl">
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-gray-900 mb-4">
            Mes Résultats
          </h1>
          <p className="text-xl text-gray-600">
            Consultez vos performances et progressez
          </p>
        </div>

        {sessions.length === 0 ? (
          <div className="bg-white rounded-lg shadow-lg p-12 text-center">
            <BarChart3 className="h-16 w-16 text-gray-400 mx-auto mb-4" />
            <h2 className="text-2xl font-semibold text-gray-900 mb-2">
              Aucun résultat disponible
            </h2>
            <p className="text-gray-600 mb-6">
              Vous n'avez pas encore passé de test. Commencez par passer votre premier test !
            </p>
            <button
              onClick={() => navigate('/test')}
              className="btn btn-primary"
            >
              Passer un test
            </button>
          </div>
        ) : (
          <div className="grid lg:grid-cols-3 gap-8">
            {/* Liste des sessions */}
            <div className="lg:col-span-1">
              <div className="bg-white rounded-lg shadow-lg p-6">
                <h2 className="text-xl font-semibold text-gray-900 mb-4">
                  Historique des tests
                </h2>
                <div className="space-y-3">
                  {sessions.map((session) => (
                    <div
                      key={session.id}
                      onClick={() => fetchSessionDetails(session.id)}
                      className={`p-4 rounded-lg border cursor-pointer transition-all hover:shadow-md ${
                        selectedSession?.id === session.id
                          ? 'border-blue-500 bg-blue-50'
                          : 'border-gray-200 hover:border-gray-300'
                      }`}
                    >
                      <div className="flex justify-between items-start mb-2">
                        <div className="flex-1">
                          <p className="font-medium text-gray-900">
                            {formatDate(session.dateDebut)}
                          </p>
                          <p className="text-sm text-gray-600">
                            {session.scoreTotal}/{session.scoreMax} points
                          </p>
                        </div>
                        <div className={`px-2 py-1 rounded-full text-xs font-medium ${getScoreBgColor(session.pourcentage)} ${getScoreColor(session.pourcentage)}`}>
                          {session.pourcentage}%
                        </div>
                      </div>
                      <div className="flex items-center text-sm text-gray-600">
                        <Clock className="h-4 w-4 mr-1" />
                        {session.estTermine ? 'Terminé' : 'En cours'}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Détails de la session sélectionnée */}
            <div className="lg:col-span-2">
              {selectedSession && (
                <div className="space-y-6">
                  {/* Score global */}
                  <div className="bg-white rounded-lg shadow-lg p-8">
                    <div className="text-center">
                      <div className={`inline-flex items-center justify-center w-20 h-20 rounded-full mb-4 ${getScoreBgColor(selectedSession.pourcentage)}`}>
                        <Trophy className={`h-10 w-10 ${getScoreColor(selectedSession.pourcentage)}`} />
                      </div>
                      <h2 className="text-3xl font-bold text-gray-900 mb-2">
                        {selectedSession.pourcentage}%
                      </h2>
                      <p className="text-xl text-gray-600 mb-4">
                        {selectedSession.scoreTotal} / {selectedSession.scoreMax} points
                      </p>
                      <div className="grid grid-cols-3 gap-4 mt-6">
                        <div className="text-center">
                          <div className="flex items-center justify-center text-green-600 mb-1">
                            <CheckCircle className="h-5 w-5 mr-1" />
                            <span className="font-semibold">
                              {selectedSession.questionsCorrectes || 0}
                            </span>
                          </div>
                          <p className="text-sm text-gray-600">Correctes</p>
                        </div>
                        <div className="text-center">
                          <div className="flex items-center justify-center text-red-600 mb-1">
                            <XCircle className="h-5 w-5 mr-1" />
                            <span className="font-semibold">
                              {selectedSession.questionsIncorrectes || 0}
                            </span>
                          </div>
                          <p className="text-sm text-gray-600">Incorrectes</p>
                        </div>
                        <div className="text-center">
                          <div className="flex items-center justify-center text-gray-600 mb-1">
                            <Clock className="h-5 w-5 mr-1" />
                            <span className="font-semibold">
                              {selectedSession.dureeTotale || 'N/A'}
                            </span>
                          </div>
                          <p className="text-sm text-gray-600">Durée</p>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Performance par thème */}
                  <div className="bg-white rounded-lg shadow-lg p-6">
                    <h3 className="text-xl font-semibold text-gray-900 mb-4">
                      Performance par thème
                    </h3>
                    <div className="space-y-4">
                      {selectedSession.resultatsParTheme?.map((theme) => (
                        <div key={theme.themeId} className="border rounded-lg p-4">
                          <div className="flex justify-between items-center mb-2">
                            <div className="flex items-center">
                              <BookOpen className="h-5 w-5 text-gray-600 mr-2" />
                              <span className="font-medium text-gray-900">
                                {theme.nomTheme}
                              </span>
                            </div>
                            <span className={`font-semibold ${getScoreColor(theme.pourcentage)}`}>
                              {theme.pourcentage}%
                            </span>
                          </div>
                          <div className="w-full bg-gray-200 rounded-full h-2">
                            <div
                              className={`h-2 rounded-full transition-all duration-300 ${
                                theme.pourcentage >= 80 ? 'bg-green-600' :
                                theme.pourcentage >= 60 ? 'bg-yellow-600' : 'bg-red-600'
                              }`}
                              style={{ width: `${theme.pourcentage}%` }}
                            ></div>
                          </div>
                          <p className="text-sm text-gray-600 mt-1">
                            {theme.scoreObtenu}/{theme.scoreMaximum} points
                          </p>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Statistiques */}
                  <div className="bg-white rounded-lg shadow-lg p-6">
                    <h3 className="text-xl font-semibold text-gray-900 mb-4">
                      Statistiques détaillées
                    </h3>
                    <div className="grid grid-cols-2 gap-6">
                      <div className="flex items-center">
                        <Calendar className="h-8 w-8 text-gray-600 mr-3" />
                        <div>
                          <p className="text-sm text-gray-600">Date du test</p>
                          <p className="font-medium text-gray-900">
                            {formatDate(selectedSession.dateDebut)}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center">
                        <Clock className="h-8 w-8 text-gray-600 mr-3" />
                        <div>
                          <p className="text-sm text-gray-600">Temps moyen par question</p>
                          <p className="font-medium text-gray-900">
                            {selectedSession.tempsMoyenParQuestion || 'N/A'}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center">
                        <TrendingUp className="h-8 w-8 text-gray-600 mr-3" />
                        <div>
                          <p className="text-sm text-gray-600">Classement</p>
                          <p className="font-medium text-gray-900">
                            {selectedSession.classement || 'N/A'}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center">
                        <Award className="h-8 w-8 text-gray-600 mr-3" />
                        <div>
                          <p className="text-sm text-gray-600">Niveau atteint</p>
                          <p className="font-medium text-gray-900">
                            {selectedSession.pourcentage >= 80 ? 'Excellent' :
                             selectedSession.pourcentage >= 60 ? 'Bon' : 'À améliorer'}
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex justify-center space-x-4">
                    <button
                      onClick={() => navigate('/test')}
                      className="btn btn-primary"
                    >
                      Passer un autre test
                    </button>
                    <button
                      className="btn btn-secondary"
                    >
                      Télécharger le rapport
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Resultats;
