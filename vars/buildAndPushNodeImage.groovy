#!/usr/bin/env groovy

/**
 * Build and push immutable and latest Docker image tags.
 *
 * @param imageRepository Docker Hub repository, for example user/application.
 * @param imageTag        Immutable application/build tag.
 * @param credentialsId   Jenkins Docker registry credential ID.
 * @param context         Docker build context.
 */
def call(
    String imageRepository,
    String imageTag,
    String credentialsId = 'dockerhub-creds',
    String context = '.'
) {
    if (!imageRepository?.trim()) {
        error('imageRepository must not be empty')
    }

    if (!imageTag?.trim()) {
        error('imageTag must not be empty')
    }

    echo "Building ${imageRepository}:${imageTag}"

    sh """
        set -eu

        docker build \
          --pull \
          --tag '${imageRepository}:${imageTag}' \
          --tag '${imageRepository}:latest' \
          '${context}'
    """

    withCredentials([
        usernamePassword(
            credentialsId: credentialsId,
            usernameVariable: 'DOCKER_USERNAME',
            passwordVariable: 'DOCKER_TOKEN'
        )
    ]) {
        sh """
            set +x

            echo "\${DOCKER_TOKEN}" |
              docker login \
                --username "\${DOCKER_USERNAME}" \
                --password-stdin

            set -x
            docker push '${imageRepository}:${imageTag}'
            docker push '${imageRepository}:latest'
            set +x

            docker logout
        """
    }
}