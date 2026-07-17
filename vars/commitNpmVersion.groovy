#!/usr/bin/env groovy

/**
 * Commit package.json and package-lock.json version changes to the current
 * multibranch pipeline branch.
 *
 * @param appDir             Directory containing npm version files.
 * @param credentialsId      Jenkins GitHub credential ID.
 * @param repositoryHostPath GitHub host/repository path without scheme.
 * @param committerName      Git commit author name.
 * @param committerEmail     Git commit author email.
 */
def call(
    String appDir = 'app',
    String credentialsId = 'github-token',
    String repositoryHostPath,
    String committerName = 'Jenkins CI',
    String committerEmail = 'jenkins@example.com'
) {
    if (!env.BRANCH_NAME?.trim()) {
        error('BRANCH_NAME is unavailable. Use this function in a multibranch pipeline.')
    }

    withCredentials([
        usernamePassword(
            credentialsId: credentialsId,
            usernameVariable: 'GITHUB_USERNAME',
            passwordVariable: 'GITHUB_TOKEN'
        )
    ]) {
        sh """
            set -eu
            set +x

            git config user.name '${committerName}'
            git config user.email '${committerEmail}'

            git remote set-url origin \
              "https://\${GITHUB_USERNAME}:\${GITHUB_TOKEN}@${repositoryHostPath}"

            set -x

            git add \
              '${appDir}/package.json' \
              '${appDir}/package-lock.json'

            if git diff --cached --quiet; then
              echo 'No npm version changes to commit.'
              exit 0
            fi

            git commit -m 'ci: bump Node.js application version [jenkins]'
            git push origin "HEAD:\${BRANCH_NAME}"

            set +x
        """
    }
} 