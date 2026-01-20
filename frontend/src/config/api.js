const API_BASE_URL = process.env.NODE_ENV === 'production' 
  ? '/api' 
  : 'http://localhost:8080/gestion-tests-backend/api';

export default API_BASE_URL;
