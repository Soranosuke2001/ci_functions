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
                    sh '. venv/bin/activate'
                    
                    sh 'pip install -r requirements.txt --break-system-packages'
                    sh 'pip install --upgrade flask --break-system-packages'
                }
            }
            stage('Python Lint') {
                steps {
                    sh '. venv/bin/activate'
                    sh 'pylint --fail-under 5 *.py'
                }
            }
            stage('Test and Coverage') {
                steps {
                    script {
                        // Remove any existing test results
                        def test_reports_exist = fileExists('test-reports')
                        if (test_reports_exist) {
                            sh 'rm -rf test-reports/*.xml'
                        }
                        
                        def api_test_reports_exist = fileExists('api-test-reports')
                        if (api_test_reports_exist) {
                            sh 'rm -rf api-test-reports/*.xml'
                        }
                    }    
                    script {
                        // Run tests with coverage
                        sh '. venv/bin/activate'

                        def testFiles = findFiles(glob: 'test*.py')
                        testFiles.each {
                            sh "coverage run --omit '*/site-packages/*,*/dist-packages/*' ${it.path}"
                        }
                    }
                    
                    script {
                        def test_reports_exist = fileExists 'test-reports'
                        if (test_reports_exist) {
                            junit 'test-reports/*.xml'
                        }
                        
                        def api_test_reports_exist = fileExists 'api-test-reports'
                        if (api_test_reports_exist) {
                            junit 'api-test-reports/*.xml'
                        }

                        sh 'coverage report'
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
                    sh "docker build -t ${dockerRepoName}:latest --tag soranosuke/${dockerRepoName}:${imageName} ."
                    sh "docker push soranosuke/${dockerRepoName}:${imageName}"
                }
                }
            }
            stage('Zip Artifacts') {
                steps {
                    script {
                        sh 'zip -r app.zip *.py'
                    }
                }
                post {
                    always {
                        archiveArtifacts artifacts: 'app.zip', onlyIfSuccessful: true
                    }
                }
            }
        }
    }
}
