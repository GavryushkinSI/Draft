const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');
const TerserPlugin = require('terser-webpack-plugin');

const optimization = (isProd) => {
    const config = {};

    if (isProd) {
        config.usedExports = 'global';
        config.minimize = true;
        config.minimizer = [new CssMinimizerPlugin(), new TerserPlugin({parallel: true, extractComments: false})];
    }

    return config;
};

module.exports = optimization;
