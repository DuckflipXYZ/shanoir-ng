const webpack = require('webpack');
const webpackMerge = require('webpack-merge');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const commonConfig = require('./webpack.common.js');
const helpers = require('./helpers');

/**
 * Webpack Constants
 */
const ENV = process.env.ENV = process.env.NODE_ENV = 'development';
const BACKEND_API_ROOT_URL = 'http://localhost/shanoir-ng';
const METADATA = webpackMerge(commonConfig.metadata, {
    host: 'localhost',
    BACKEND_API_USERS_MS_URL: BACKEND_API_ROOT_URL + '/users',
    BACKEND_API_CENTERS_MS_URL: BACKEND_API_ROOT_URL + '/centers',
    KEYCLOAK_BASE_URL: 'http://localhost/auth',
    LOGOUT_REDIRECT_URL: 'http://localhost/shanoir-ng/index.html',
    port: 8081,
    ENV: ENV,
});

module.exports = webpackMerge(commonConfig, {
    devtool: 'source-map',

    output: {
        path: helpers.root('dist'),
        publicPath: '/',
        filename: '[name].js',
        chunkFilename: '[id].chunk.js'
    },

    module: {
        rules: [
            {
                test: /\.ts$/,
                loaders: [{
                    loader: 'awesome-typescript-loader',
                    options: { configFileName: helpers.root('.', 'tsconfig.json') }
                }, 'angular2-template-loader']
            }
        ]
    },

    plugins: [
        new HtmlWebpackPlugin({
            filename: 'index.html',
            template: 'src/index.html'
        }),

        new webpack.NoEmitOnErrorsPlugin(),
        new webpack.optimize.UglifyJsPlugin({ // https://github.com/angular/angular/issues/10618
            mangle: {
                keep_fnames: true
            }
        }),
        new ExtractTextPlugin('[name].css'),
        new webpack.DefinePlugin({
            'process.env': {
                'ENV': JSON.stringify(ENV)
            }
        }),
        new webpack.LoaderOptionsPlugin({
            htmlLoader: {
                minimize: false // workaround for ng2
            }
        }),

        /**
         * Plugin: DefinePlugin
         * Description: Define free variables.
         * Useful for having development builds with debug logging or adding global constants.
         *
         * Environment helpers
         *
         * See: https://webpack.github.io/docs/list-of-plugins.html#defineplugin
         */
        new webpack.DefinePlugin({
            'ENV': JSON.stringify(METADATA.ENV),
            'BACKEND_API_USERS_MS_URL': JSON.stringify(METADATA.BACKEND_API_USERS_MS_URL),
            'BACKEND_API_CENTERS_MS_URL': JSON.stringify(METADATA.BACKEND_API_CENTERS_MS_URL),
            'KEYCLOAK_BASE_URL': JSON.stringify(METADATA.KEYCLOAK_BASE_URL),
            'LOGOUT_REDIRECT_URL': JSON.stringify(METADATA.LOGOUT_REDIRECT_URL),
            'process.env': {
                'ENV': JSON.stringify(METADATA.ENV),
                'NODE_ENV': JSON.stringify(METADATA.ENV),
                'BACKEND_API_USERS_MS_URL': JSON.stringify(METADATA.BACKEND_API_USERS_MS_URL),
                'BACKEND_API_CENTERS_MS_URL': JSON.stringify(METADATA.BACKEND_API_CENTERS_MS_URL),
                'LOGOUT_REDIRECT_URL': JSON.stringify(METADATA.LOGOUT_REDIRECT_URL),
                'KEYCLOAK_BASE_URL': JSON.stringify(METADATA.KEYCLOAK_BASE_URL),
            }
        })
    ]
});