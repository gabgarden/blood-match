import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const base = (env.VITE_BASE || '/').trim() || '/'

  return {
    base,
    plugins: [
      react(),
      VitePWA({
        registerType: 'autoUpdate',
        includeAssets: ['favicon.svg', 'gota-de-sangue.png', 'pwa-icon.svg'],
        manifest: {
          name: 'BloodMatch - Conectando Vidas',
          short_name: 'BloodMatch',
          description: 'Plataforma Inteligente de Doação de Sangue e Hemocentros',
          theme_color: '#ae131a',
          background_color: '#f9f9fb',
          display: 'standalone',
          orientation: 'portrait',
          start_url: './',
          scope: './',
          icons: [
            {
              src: 'pwa-icon.svg',
              sizes: '192x192 512x512',
              type: 'image/svg+xml',
              purpose: 'any'
            },
            {
              src: 'gota-de-sangue.png',
              sizes: '192x192 512x512',
              type: 'image/png',
              purpose: 'maskable'
            }
          ]
        },
        workbox: {
          globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}']
        }
      })
    ],
    server: {
      host: true,
      port: 5173,
      watch: {
        usePolling: true,
      },
    },
  }
})
