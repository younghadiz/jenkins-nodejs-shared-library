#!/usr/bin/env groovy

/**
 * Increment the npm application version and expose APP_VERSION and IMAGE_TAG.
 *
 * @param appDir       Directory containing package.json.
 * @param releaseType  npm release type: patch, minor, or major.
 */ 
def call(String appDir = 'app', String releaseType = 'minor') {
    List<String> allowedReleaseTypes = ['patch', 'minor', 'major']

    if (!allowedReleaseTypes.contains(releaseType)) {
        error(
            "Unsupported npm release type '${releaseType}'. " +
            "Allowed values: ${allowedReleaseTypes.join(', ')}"
        )
    }

    dir(appDir) {
        echo "Incrementing npm ${releaseType} version in ${appDir}"

        sh """
            set -eu
            npm version '${releaseType}' --no-git-tag-version
        """

        Map packageJson = readJSON file: 'package.json'
        String version = packageJson.version?.toString()?.trim()

        if (!version) {
            error('Unable to read the updated version from package.json')
        }

        env.APP_VERSION = version
        env.IMAGE_TAG = "${version}-${env.BUILD_NUMBER}"

        echo "Application version: ${env.APP_VERSION}"
        echo "Image tag: ${env.IMAGE_TAG}"
    }
}
