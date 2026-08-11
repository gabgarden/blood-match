export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        primary: "#ae131a",
        secondary: "#4c616c",
        background: "#f9f9fb",
        surface: "#f9f9fb",
        "surface-container-low": "#f3f3f5",
        "surface-container-high": "#e8e8ea",
        "surface-container-highest": "#e2e2e4",
        "surface-container-lowest": "#ffffff",
        "secondary-container": "#cfe6f2",
        "outline-variant": "#e4beba",
        "on-primary": "#ffffff",
        "on-surface": "#1a1c1d",
        "text-secondary": "#444748",
      },
      fontFamily: {
        headline: ["Manrope"],
        body: ["Public Sans"],
      },
    },
  },
  plugins: [],
};