/**
 * Configuracion de Tailwind CSS (la misma que usaba el CDN en SistemaInventario.html).
 * La interfaz ya no descarga Tailwind de internet: usa el archivo compilado
 * src/main/resources/html/css/tailwind.css.
 *
 * Si se agregan clases nuevas al HTML, regenerar con el CLI standalone v3.4.17:
 *   tailwindcss -c tailwind.config.js -i src/main/resources/html/css/tailwind.input.css \
 *               -o src/main/resources/html/css/tailwind.css --minify
 */
module.exports = {
  content: ['./src/main/resources/html/**/*.html', './src/main/resources/html/js/**/*.js'],
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: { sans: ['Plus Jakarta Sans', 'sans-serif'] },
      colors: {
        brand: {
          50: '#fdf8f0', 100: '#faefd9', 200: '#f4d9a8', 300: '#ecbd6c', 400: '#e29d38',
          500: '#d4821b', 600: '#c06a10', 700: '#a05010', 800: '#7e3f14', 900: '#663514',
        },
      },
    },
  },
};
