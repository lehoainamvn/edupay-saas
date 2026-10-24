// src/environments/environment.prod.ts
// ─── Angular Environment: PRODUCTION ─────────────────────────────────────
// Áp dụng khi: ng build --configuration=production
// Angular CLI tự động dùng file này thay thế environment.ts khi build prod
// ──────────────────────────────────────────────────────────────────────────

export const environment = {
  production: true,
  envName: 'production',

  // Backend API trên server thật (thay bằng domain của bạn)
  apiBaseUrl: 'https://api.edupay.vn/api',

  // VietQR API (Napas247 - Miễn phí)
  vietqrImageUrl: 'https://img.vietqr.io/image',

  // App metadata
  appName: 'EduPay',
  appVersion: '1.0.0',

  // Feature Flags (tắt debug)
  devFeatureFlags: {
    showDebugPanel: false,
    enableAllFeatures: false,  // PROD: tính năng theo gói thuê bao từ API
  }
};
