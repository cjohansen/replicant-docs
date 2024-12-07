/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.clj", "./src/**/*.cljc", "./src/**/*.cljs", "./portfolio/**/*.cljs"],
  theme: {
    extend: {
      colors: {
        whitish: "rgb(235, 236, 240)",
        yellow: "#ffcb6b",
        red: "#f66",
        green: "#a5e844",
        cljblue: "#6180D2",
        cljlightblue: "#96B1F5",
        cljgreen: "#76AF47",
        cljlightgreen: "#A1D85F"
      },
      typography: theme => ({
        DEFAULT: {
          css: {
            a: {
              color: theme('colors.blue.600')
            },
            'a:hover': {
              color: theme('colors.blue.500')
            }
          }
        },
        invert: {}
      }),
      fontFamily: {
        sans: 'Lato, ui-sans-serif, system-ui, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji"',
        mono: '"Source Code Pro", ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace'
      }
    }
  },
  plugins: [
    require('@tailwindcss/typography'),
    require("daisyui")
  ],
  daisyui: {
    themes: [
      {
        dark: {
          ...require("daisyui/src/theming/themes")["dark"],
          "primary": "#00b3f0",
          "secondary": "#a5e844",
        }
      },
      "cupcake"
    ]
  }
}
