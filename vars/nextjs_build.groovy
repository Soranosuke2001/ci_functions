def call(Map params) {
    def dockerRepoName = params.dockerRepoName
    def imageName = params.imageName
    def serviceName = params.serviceName

    def remote = [:]
    remote.name = 'microservices-project'
    remote.host = '35.230.127.229'
    remote.allowAnyHosts = true

    pipeline {
        agent any

        parameters {
            booleanParam(defaultValue: false, description: 'Deploy the App', name:'DEPLOY')
        }

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
                        sh """
                        node --version
                        npm --version
                        npm install ./${serviceName}/
                        """
                    }
                }
            }

            stage('Build') {
                steps {
                    script {
                        // Build the Next.js project
                        sh """
                        cd ./${serviceName}
                        ls -l
                        npm run build
                        """
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
                    withCredentials([sshUserPrivateKey(credentialsId: 'deployment_vm', keyFileVariable: 'KEY_FILE', usernameVariable: 'USER')]) {
                        script {
                            remote.user = USER
                            remote.identityFile = KEY_FILE

                            sshPut remote: remote, from: 'deployment/docker-compose.yml', into: '/home/soranosuke/deployment/docker-compose.yml'
                            writeFile file: 'deployment.sh', text: '''
                            cd deployment
                            docker compose stop
                            docker compose rm -f
                            docker compose pull
                            docker compose up -d
                            '''
                            sshScript remote: remote, script: "deployment.sh"
                        }
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
}
