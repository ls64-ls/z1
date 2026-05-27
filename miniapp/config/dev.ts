export default {
  env: { NODE_ENV: '"development"' },
  defineConstants: {
    'process.env.API_BASE_URL': JSON.stringify('http://127.0.0.1:8088/api/v1')
  },
  mini: {},
  h5: {}
}
