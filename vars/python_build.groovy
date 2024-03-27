def call(dockerRepoName, imageName, serviceName) {
    def remote = [:]
    remote.name = 'microservices-project'
    remote.host = '35.230.127.229'
    remote.allowAnyHosts = true

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
                            pip install bandit
                            pip install -r requirements.txt
                        '''
                    }
                }
            }
            stage('Lint') {
                steps {
                    script {
                        // Run pylint
                        sh ". venv/bin/activate && pylint --fail-under=5 ${serviceName}/*.py"
                    }
                }
            }
            stage('Security Check') {
                steps {
                    script {
                        // Install Safety tool
                        sh ". venv/bin/activate && bandit -r ${serviceName}/"
                            // safety check -r requirements.txt
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
                    // SSH into the VM and deploy the app
                    withCredentials([sshUserPrivateKey(credentialsId: 'microservices_vm', keyFileVariable: 'KEY_FILE', usernameVariable: 'USER')]) {
                        script {
                            remote.user = USER
                            remote.identityFile = KEY_FILE

                            sshPut remote: remote, from: 'deployment/docker-compose.yml', into: '/home/soranosuke/deployment/docker-compose.yml'
                            writeFile file: 'deployment.sh', text: 'cd deployment && docker compose up -d'
                            sshScript remote: remote, script: "deployment.sh"
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
}
