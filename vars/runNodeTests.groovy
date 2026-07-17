#!/usr/bin/env groovy

/**
 * Install exact locked dependencies and run the project's Jest tests.
 *
 * @param appDir Directory containing package.json.
 */
def call(String appDir = 'app') {
    dir(appDir) {
        echo "Installing dependencies in ${appDir}"

        sh '''
            set -eu
            npm ci
        '''

        echo 'Running Node.js tests'

        sh '''
            set -eu
            npm test -- --runInBand
        '''
    }
} 