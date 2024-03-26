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
            stage ('Lint') {
                steps {
                    script {
                        // Run pylint
                        sh '. venv/bin/activate && pylint Receiver/*.py || true'
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
