export default {
  env: { NODE_ENV: '"production"' },
  defineConstants: {
    'process.env.API_BASE_URL': JSON.stringify('https://your-domain.com/api/v1')
  },
  mini: {},
  h5: {}
}
