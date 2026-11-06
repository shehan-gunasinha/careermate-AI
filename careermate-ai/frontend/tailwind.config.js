/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        navy: { 50: '#eef1fb', 100: '#d9dff4', 200: '#b3bfe9', 500: '#2b3a8c', 600: '#1f2c6f', 700: '#18235a', 800: '#111a45', 900: '#0b1230' },
        brand: { 50: '#f5f0ff', 100: '#ebe0ff', 200: '#d5bfff', 500: '#7c3aed', 600: '#6d28d9', 700: '#5b21b6' },
        accent: { 50: '#e6fbf8', 100: '#c4f5ee', 500: '#14b8a6', 600: '#0d9488', 700: '#0f766e' },
        slate: { 950: '#0f172a' },
      },
      fontFamily: { sans: ['Inter', 'ui-sans-serif', 'system-ui', 'Segoe UI', 'Roboto', 'sans-serif'] },
      boxShadow: { card: '0 1px 2px rgba(15,23,42,.05), 0 4px 16px rgba(15,23,42,.06)' },
    },
  },
  plugins: [],
};
