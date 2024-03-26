def call(dockerRepoName, imageName) {
    pipeline {
        agent any
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
                        sh '. venv/bin/activate && pylint Receiver/*.py || true'
                    }
                }
            }
            stage('Security Check') {
                steps {
                    script {
                        // Install Safety tool
                        sh '''
                            . venv/bin/activate
                            pip install safety
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
                    withCredentials([string(credentialsId: 'DockerHub', variable: 'TOKEN')]) {
                        sh "docker login -u 'soranosuke' -p '$TOKEN' docker.io"
                        sh "docker build -t ${dockerRepoName}:latest --tag soranosuke/${dockerRepoName}:${imageName} Receiver/."
                        sh "docker push soranosuke/${dockerRepoName}:${imageName}"
                    }
                }
            }
        }
        post {
            always {
                script {
                    // Clean up virtual environment
                    sh 'deactivate || true' // Deactivate virtual environment
                    sh 'rm -rf venv' // Remove virtual environment
                }
            }
        }
    }
}
