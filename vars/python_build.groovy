def call(dockerRepoName, imageName) {
    pipeline {
        agent any
        stages {
            stage('Setup') {
                steps {
                    script {
                        // Create and activate virtual environment
                        sh 'python3 -m venv venv'
                        sh '. venv/bin/activate'
                        // Upgrade pip to avoid potential issues
                        sh 'pip install --upgrade pip'
                        // Install flask and other dependencies
                        sh 'pip install --upgrade flask'
                        sh 'pip install mysqlclient'
                        sh 'pip install -r requirements.txt'
                    }
                }
            }
            stage ('Lint') {
                steps {
                    script {
                        // Run pylint
                        sh 'pylint Receiver/*.py || true' // Continue pipeline execution even if pylint fails
                    }
                }  
            }
            // Add other stages as per your requirements
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
