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
            stage('File Transfer') {
                when {
                    expression { params.DEPLOY == true }
                }
                steps {
                    script {
                        // Transfer the App to the remote VM
                        sshPut remote: remote, from: '/home/soranosuke/Microservices/deployment/docker-compose.yml', into: '/home/soranosuke/deployment/docker-compose.yml'
                    }
                }
            }
            stage('Deploy') {
                when {
                    expression { params.DEPLOY == true }
                }
                steps {
                    script {
                        // Execute docker-compose up -d on the remote VM
                        sshScript remote: remote, script: '''
                        cd /home/soranosuke/deployment
                        docker-compose up -d
                    '''
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
}

def remote = [
    // IP address or hostname of the remote VM
    remote: '35.230.127.229',
    // Credentials ID added in Jenkins for SSH key authentication
    credentialsId: 'microservices_vm'
]
