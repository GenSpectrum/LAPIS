/** @type {import('tailwindcss').Config} */
module.exports = {
	content: ['./src/**/*.{astro,html,js,jsx,md,mdx,svelte,ts,tsx,vue}'],
  corePlugins:{
    // TODO: see https://github.com/withastro/starlight/issues/88#issuecomment-1561272435
    //  and https://github.com/withastro/starlight/pull/337
    preflight: false, // disabling preflight styles
  },
	theme: {
		extend: {},
	},
	plugins: [require('daisyui')],
  daisyui: {
    logs: false,
  }
}
