const path = require('path');
const optimization = require("./optimization");
const PROJECT_NAME = 'draft-ui';

const isProduction = (
    process.argv.some(arg => arg === '-p' || arg === '--production') ||
    process.env.NODE_ENV === 'production'
);

const config = {
    mode: isProduction ? 'production' : 'development',
    optimization: optimization(isProduction),
    devtool: !isProduction ? 'source-map' : false,
    entry: "./src/index.tsx",
    module: {
        rules: [
            {
                test: /\.(ts|js)x?$/,
                exclude: /node_modules/,
                use: {
                    loader: "babel-loader",
                    options: {
                        presets: [
                            "@babel/preset-env",
                            "@babel/preset-react",
                            "@babel/preset-typescript",
                        ],
                    },
                },
            },
            {
                test: /\.css$/,
                use: ['style-loader', 'css-loader']
            },
            {
                test: /\.(woff|woff2|eot|ttf|otf|svg)$/,
                use: [{
                    loader: 'file-loader',
                    options: {
                        outputPath: 'fonts/',
                    },
                }]
            }
        ],
    },
    resolve: {
        extensions: ['.js', '.ts', '.tsx', '.css', '.png'],
    },
    output: {
        path: path.resolve(__dirname, "build"),
        filename: "bundle.js",
        chunkFilename: isProduction ? 'js/[contenthash].js' : undefined,
        libraryTarget: 'umd',
        clean: true,
    },
    devServer: {
        static: path.join(__dirname, "build"),
        compress: true,
        port: 3000,
    },
};

module.exports = config;