def call(dockerRepoName, imageName) {
    pipeline {
        agent any
        stages {
            stage('Build') {
                steps {
                    script {
                        if (fileExists('venv')) {
                            sh 'rm -rf venv'
                        }
                    }

                    sh 'python3 -m venv venv'
                    sh 'source venv/bin/activate'

                    sh 'pip install --upgrade flask'
                    sh 'pip install -r requirements.txt'
                }
            }
            stage ('PyLint') {
                steps {
                    script {
                        sh 'pylint Receiver/*.py'
                    }
                }  
            }
        }
    }
}
