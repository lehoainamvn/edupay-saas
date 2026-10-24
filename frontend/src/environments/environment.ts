// src/environments/environment.ts
// ─── Angular Environment: DEV ──────────────────────────────────────────────
// Áp dụng khi: ng serve  (mặc định)
// Được build với: ng build --configuration=development
// ──────────────────────────────────────────────────────────────────────────

export const environment = {
  production: false,
  envName: 'development',

  // Backend API chạy local Spring Boot
  apiBaseUrl: 'http://localhost:8080/api',

  // VietQR API (Napas247 - Miễn phí)
  vietqrImageUrl: 'https://img.vietqr.io/image',

  // App metadata
  appName: 'EduPay Dev',
  appVersion: '1.0.0-dev',

  // Feature Flags cho FE (đồng bộ với plan_features ở Backend)
  // Trong prod, các flag này sẽ được load từ API response
  devFeatureFlags: {
    showDebugPanel: true,
    enableAllFeatures: true,   // DEV: bật tất cả tính năng để test UI
  }
};
