/**
 * Design tokens (see DESIGN.md). Colours are authored in OKLCH so lightness steps are perceptually
 * even; neutrals carry a faint tint of the brand hue (185, teal) for cohesion.
 * @type {import('tailwindcss').Config}
 */
const ramp = (hue, steps) =>
  Object.fromEntries(Object.entries(steps).map(([k, [l, c]]) => [k, `oklch(${l} ${c} ${hue} / <alpha-value>)`]));

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Be Vietnam Pro"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      colors: {
        // Action, selection, focus and wayfinding only.
        brand: ramp(185, {
          50: [0.975, 0.018],
          100: [0.945, 0.035],
          200: [0.89, 0.06],
          300: [0.8, 0.09],
          400: [0.68, 0.11],
          500: [0.58, 0.11],
          600: [0.5, 0.1],
          700: [0.44, 0.09],
          800: [0.37, 0.075],
          900: [0.31, 0.06],
        }),
        // Tinted neutral ramp. 500 and darker meet 4.5:1 on white for text.
        slate: ramp(185, {
          50: [0.985, 0.003],
          100: [0.965, 0.005],
          200: [0.925, 0.007],
          300: [0.87, 0.01],
          400: [0.7, 0.012],
          500: [0.52, 0.014],
          600: [0.44, 0.014],
          700: [0.37, 0.013],
          800: [0.28, 0.012],
          900: [0.21, 0.01],
          950: [0.15, 0.008],
        }),
      },
      transitionTimingFunction: {
        'out-expo': 'cubic-bezier(0.16, 1, 0.3, 1)',
      },
      keyframes: {
        'fade-in': { from: { opacity: '0' }, to: { opacity: '1' } },
        'dialog-in': {
          from: { opacity: '0', transform: 'translateY(8px) scale(0.98)' },
          to: { opacity: '1', transform: 'none' },
        },
      },
      animation: {
        'fade-in': 'fade-in 150ms ease-out',
        'dialog-in': 'dialog-in 220ms cubic-bezier(0.16, 1, 0.3, 1)',
      },
    },
  },
  plugins: [],
};
