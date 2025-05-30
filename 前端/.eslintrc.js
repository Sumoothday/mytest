module.exports = {
  env: {
  browser: true,
  es6: true,
  jest: true
},
parserOptions: {
  ecmaVersion: 2021,  // 这一行才真正控制 ES 的语法版本
  sourceType: 'module'
},

  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended'
  ],
  parserOptions: {
    ecmaVersion: 12,
    sourceType: 'module'
  },
  rules: {
    // 你可以根据需要自定义规则
    semi: ['error', 'always'],
    quotes: ['error', 'single']
  }
};
