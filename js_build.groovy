def call(dockerRepoName, imageName, serviceName) {
pipeline {
    agent any

    environment {
        // Define environment variables
        NODE_VERSION = '16'  // Define the Node.js version to use
    }

    stages {
        stage('Preparation') {
            steps {
                script {
                    // Checkout the Git repository
                    checkout scm
                }
            }
        }

        stage('Install dependencies') {
            steps {
                script {
                    // Use Node Version Manager (nvm) to manage Node.js versions
                    sh """
                    echo "Installing NVM and Node.js"
                    curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.1/install.sh | bash
                    export NVM_DIR="$HOME/.nvm"
                    [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
                    [ -s "$NVM_DIR/bash_completion" ] && \. "$NVM_DIR/bash_completion"
                    nvm install $NODE_VERSION
                    nvm use $NODE_VERSION
                    node --version
                    npm --version
                    """
                    // Install npm dependencies
                    sh 'npm install'
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    // Build the Next.js project
                    sh 'npm run build'
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    // Run tests
                    sh 'npm run test'
                }
            }
        }

        stage('Package') {
            when {
                allOf {
                    branch: "main"

                }
                expression { env.GIT_BRANCH == 'origin/main' }
            }
            steps {
                // Pushing the image to Docker Hub
                withCredentials([string(credentialsId: 'DockerHub', variable: 'TOKEN')]) {
                    sh "docker login -u 'soranosuke' -p '$TOKEN' docker.io"
                    sh "docker build -t ${dockerRepoName}:latest --tag soranosuke/${dockerRepoName}:${imageName} ${serviceName}/."
                    sh "docker push soranosuke/${dockerRepoName}:${imageName}"
                }
            }
        }
    }

    post {
        success {
            // Actions to perform on success
            echo 'Build and tests succeeded!'
        }
        failure {
            // Actions to perform if the build fails
            echo 'Build or tests failed.'
        }
        always {
            // Actions to perform after every execution
            echo 'Pipeline execution complete.'
        }
    }
}

