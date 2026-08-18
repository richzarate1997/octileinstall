import defaultTheme from 'tailwindcss/defaultTheme';

// Overrides Tailwind's built-in slate/blue shade *values* (not new color names) with the
// main site's real design tokens (css/styles.css :root). Every existing bg-slate-950,
// text-slate-400, bg-blue-600, etc. across the dashboard picks up the real brand hex
// automatically - no need to rewrite every className. Only the shades actually used in
// admin-dashboard/src are overridden; interpolated stops (700/500/300/200) bridge gaps the
// brand's own 3-stop navy scale + single grey don't define, kept in the same progression.
const navy = { 950: '#0a0e1a', 900: '#111827', 800: '#1c2535', 700: '#2d3a52' };
const brandBlue = { 600: '#1a6fd4', 500: '#3d9bff', 400: '#3d9bff' };
const muted = { 500: '#64748f', 400: '#8a9ab5', 300: '#c1cbdf', 200: '#dde3f0' };

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        slate: { ...navy, ...muted },
        blue: brandBlue,
      },
      fontFamily: {
        // Mirrors css/styles.css's --font-display/--font-body/--font-label exactly.
        display: ['"Bebas Neue"', ...defaultTheme.fontFamily.sans],
        body: ['Barlow', ...defaultTheme.fontFamily.sans],
        label: ['"Barlow Condensed"', ...defaultTheme.fontFamily.sans],
      },
      borderRadius: {
        // The main site uses a near-flat 2px radius throughout, not Tailwind's default
        // rounded curves - overriding the scale keeps every existing rounded/rounded-lg
        // class brand-correct without touching component markup.
        DEFAULT: '2px',
        sm: '2px',
        md: '2px',
        lg: '2px',
        xl: '2px',
      },
    },
  },
  plugins: [],
};
