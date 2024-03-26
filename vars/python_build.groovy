def call(dockerRepoName, imageName, serviceName) {
    pipeline {
        agent any

        parameters {
            booleanParam(defaultValue: false, description: 'Deploy the App', name:'DEPLOY')
        }

        stages {
            stage('Setup') {
                steps {
                    script {
                        // Create and activate virtual environment
                        sh '''
                            python3 -m venv venv
                            . venv/bin/activate
                            pip install --upgrade pip
                            pip install --upgrade flask
                            pip install -r requirements.txt
                        '''
                    }
                }
            }
            stage('Lint') {
                steps {
                    script {
                        // Run pylint
                        sh ". venv/bin/activate && pylint ${serviceName}/*.py || true"
                    }
                }
            }
            stage('Security Check') {
                steps {
                    script {
                        // Install Safety tool
                        sh '''
                            . venv/bin/activate
                            safety check -r requirements.txt
                        '''
                    }
                }
            }
            stage('Package') {
                when {
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
            stage('Deploy') {
                when {
                    expression { params.DEPLOY == true }
                }
                steps {
                    withCredentials([sshUserPrivateKey(credentialsId: 'microservices_vm', keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER')]) {
                        // Transfer docker-compose.yml
                        sh """
                            scp -i \$SSH_KEY /home/soranosuke/Microservices-Project/deployment/docker-compose.yml \$SSH_USER@35.230.127.229:/home/soranosuke/assignment3/docker-compose.yml
                        """

                        sh """
                            ssh -i \$SSH_KEY \$SSH_USER@35.230.127.229 'cd /home/soranosuke/assignment3 && docker-compose up -d'
                        """
                    }
                }
            }
            post {
                always {
                    script {
                        // Clean up virtual environment
                        sh 'rm -rf venv' // Remove virtual environment
                    }
                }
            }
        }
    }
}
