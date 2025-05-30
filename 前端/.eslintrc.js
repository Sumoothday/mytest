// eslint-disable-next-line no-undef
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
overrides: [
  {
    files: ['test/unit/jest.conf.js'],
    env: {
      node: true
    }
  }
],
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended'
  ],
  rules: {
    // 你可以根据需要自定义规则
    semi: ['error', 'always'],
    quotes: ['error', 'single']
  }
};
