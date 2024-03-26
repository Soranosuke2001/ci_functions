def call(dockerRepoName, imageName, serviceName) {
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
                    withCredentials([string(credentialsId: 'DockerHub', variable: 'TOKEN')]) {
                        sh "docker login -u 'soranosuke' -p '$TOKEN' docker.io"
                        sh "docker build -t ${dockerRepoName}:latest --tag soranosuke/${dockerRepoName}:${imageName} ${serviceName}/."
                        sh "docker push soranosuke/${dockerRepoName}:${imageName}"
                    }
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
