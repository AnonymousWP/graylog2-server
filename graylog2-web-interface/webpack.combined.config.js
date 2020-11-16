const glob = require('glob');
const path = require('path');
const merge = require('webpack-merge');
const ForkTsCheckerWebpackPlugin = require('fork-ts-checker-webpack-plugin');

const TARGET = process.env.npm_lifecycle_event;

const pluginConfigPattern = 'graylog-plugin-*/**/webpack.config.js';
const globCwd = '../..';
const globOptions = {
  ignore: '**/node_modules/**',
  cwd: globCwd,
  nodir: true,
};

function isNotDependency(pluginConfig) {
  // Avoid including webpack configs of dependencies and built files.
  return !pluginConfig.includes('/target/') && !pluginConfig.includes('/node_modules/');
}

const pluginConfigs = process.env.disable_plugins === 'true' ? [] : glob.sync(pluginConfigPattern, globOptions)
  .map((config) => `${globCwd}/${config}`)
  .filter(isNotDependency);

process.env.web_src_path = path.resolve(__dirname);

// eslint-disable-next-line import/no-dynamic-require
const webpackConfig = require(path.resolve(__dirname, './webpack.config.js'));

const mergedPluginConfigs = pluginConfigs
  // eslint-disable-next-line global-require,import/no-dynamic-require
  .map((configFile) => require(configFile))
  .reduce((config, pluginConfig) => merge.smart(config, pluginConfig), {});

const mergedWebpackConfig = (TARGET === 'start')
  ? merge(webpackConfig, {
    plugins: [
      new ForkTsCheckerWebpackPlugin(),
    ],
  })
  : webpackConfig;

const finalConfig = merge.smart(mergedPluginConfigs, mergedWebpackConfig);

// We need to inject webpack-hot-middleware to all entries, ensuring the app is able to reload on changes.
if (TARGET === 'start') {
  const hmrEntries = {};
  const webpackHotMiddlewareEntry = 'webpack-hot-middleware/client?reload=true';

  Object.keys(finalConfig.entry).forEach((entryKey) => {
    const entryValue = finalConfig.entry[entryKey];
    const hmrValue = [webpackHotMiddlewareEntry];

    if (Array.isArray(entryValue)) {
      hmrValue.push(...entryValue);
    } else {
      hmrValue.push(entryValue);
    }

    hmrEntries[entryKey] = hmrValue;
  });

  finalConfig.entry = hmrEntries;
}

module.exports = finalConfig;
