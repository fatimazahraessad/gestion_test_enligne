const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  app.use(
    '/api',
    createProxyMiddleware({
      target: 'http://localhost:8080/gestion-tests-backend',
      changeOrigin: true,
      secure: false,
      logLevel: 'debug'
    })
  );
};
