#!/usr/bin/env groovy

/**
 * Deploy a versioned Node.js Docker image to an EC2 host.
 *
 * @param imageRepository     Docker registry repository.
 * @param imageTag            Immutable Docker image tag.
 * @param ec2Host             EC2 public hostname or IP.
 * @param sshCredentialsId    Jenkins SSH private-key credential ID.
 * @param remoteUser          EC2 SSH user.
 * @param deploymentDirectory Remote application directory.
 * @param composeFile         Local Docker Compose file.
 * @param deploymentScript    Local remote-execution script.
 */
def call(
    String imageRepository,
    String imageTag,
    String ec2Host,
    String sshCredentialsId = 'ec2-server-key',
    String remoteUser = 'ec2-user',
    String deploymentDirectory = '/opt/nodejs-aws-jenkins',
    String composeFile = 'deploy/docker-compose.yaml',
    String deploymentScript = 'deploy/server-commands.sh'
) {
    if (!imageRepository?.trim()) {
        error('imageRepository must not be empty')
    }

    if (!imageTag?.trim()) {
        error('imageTag must not be empty')
    }

    if (!ec2Host?.trim()) {
        error('ec2Host must not be empty')
    }

    if (!sshCredentialsId?.trim()) {
        error('sshCredentialsId must not be empty')
    }

    if (!remoteUser?.trim()) {
        error('remoteUser must not be empty')
    }

    if (!deploymentDirectory?.trim()) {
        error('deploymentDirectory must not be empty')
    }

    if (!fileExists(composeFile)) {
        error("Docker Compose file not found: ${composeFile}")
    }

    if (!fileExists(deploymentScript)) {
        error("Deployment script not found: ${deploymentScript}")
    }

    String remoteTarget = "${remoteUser}@${ec2Host}"

    echo """
Deployment details:
  Image:                ${imageRepository}:${imageTag}
  Remote host:          ${remoteTarget}
  Deployment directory:${deploymentDirectory}
  Compose file:         ${composeFile}
  Deployment script:    ${deploymentScript}
""".stripIndent().trim()

    sshagent(credentials: [sshCredentialsId]) {
        sh(
            label: 'Deploy application to EC2',
            script: """
                set -Eeuo pipefail

                echo "Testing SSH connection..."
                ssh \
                  -o BatchMode=yes \
                  -o ConnectTimeout=15 \
                  '${remoteTarget}' \
                  'echo "SSH connection successful."'

                echo "Checking remote Docker environment..."
                ssh \
                  -o BatchMode=yes \
                  -o ConnectTimeout=15 \
                  '${remoteTarget}' \
                  'docker --version &&
                  docker compose version &&
                  docker ps >/dev/null'

                echo "Preparing deployment directory..."
                ssh \
                  -o BatchMode=yes \
                  -o ConnectTimeout=15 \
                  '${remoteTarget}' \
                  'sudo mkdir -p "${deploymentDirectory}" &&
                   sudo chown "${remoteUser}:${remoteUser}" "${deploymentDirectory}" &&
                   sudo chmod 750 "${deploymentDirectory}"'

                echo "Copying Docker Compose file..."
                scp \
                  -o BatchMode=yes \
                  -o ConnectTimeout=15 \
                  '${composeFile}' \
                  '${remoteTarget}:${deploymentDirectory}/docker-compose.yaml'

                echo "Copying deployment script..."
                scp \
                  -o BatchMode=yes \
                  -o ConnectTimeout=15 \
                  '${deploymentScript}' \
                  '${remoteTarget}:${deploymentDirectory}/server-commands.sh'

                echo "Setting deployment script permissions..."
                ssh \
                  -o BatchMode=yes \
                  -o ConnectTimeout=15 \
                  '${remoteTarget}' \
                  'chmod 750 "${deploymentDirectory}/server-commands.sh"'

                echo "Running remote deployment..."
                ssh \
                  -o BatchMode=yes \
                  -o ConnectTimeout=15 \
                  '${remoteTarget}' \
                  'DEPLOYMENT_DIRECTORY="${deploymentDirectory}" \
                   "${deploymentDirectory}/server-commands.sh" \
                   "${imageRepository}" \
                   "${imageTag}"'
            """
        )
    }
}