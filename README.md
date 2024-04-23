# Setup

In this project I will be using a docker container to run the Jenkins Master machine. The dockerfile contained in this repo will be used to create the docker container.

## Pre-Requirements

There are a couple of things that must be completed before setting up the Jenkins docker container.

1. Ensure that docker is installed and all docker commands can be run without using `sudo`

2. Ensure that Git is installed and this repo is cloned to the machine.

Note: The entire repo does not have to cloned, but only the dockerfile is required.

3. Ensure that port 8080 is open on the machine. The Jenkins container will be running on port 8080.

## Jenkins Setup

This section will cover the setup process to configure Jenkins.

### Jenkins Docker Container Setup

1. Create a new directory on the machine and copy the dockerfile over to the new directory.

Note: The maintainer in the dockerfile can be changed to your personal email.

2. `cd` into the new directory and run the following command to build the docker image.

```bash
docker build -t myjenkins .
```

Note: The name, `myjenkins` will be set as the name of the image and can be named something else.

3. Create a container from the docker image by using the following command,

```bash
docker run -p 8080:8080 -p 50000:50000 --restart always --name=jenkins-master --mount source=jenkins-log,target=/var/log/jenkins --mount source=jenkins-data,target=/var/jenkins_home -v /var/run/docker.sock:/var/run/docker.sock -d myjenkins
```

Note: The `--name` option will set the name of the container and can be set to something else.

Note: The `-d` option specifies which image to use, so if the image name is different, use the image name you set to instead.

4. Run the container in interactive mode by using the following command.

```bash
docker exec -it jenkins-master bash
```

5. Find the default admin password within the container. We will need later when first setting up Jenkins.

```bash
cat $JENKINS_HOME/secrets/initialAdminPassword
```

6. Test the Jenkins application by visiting its website.

URL: `<public-ip-address>:8080`

Note: You should be able to see a Jenkins and you will be prompted to enter the default admin password to continue the setup.

### Jenkins Plugin Setup

1. Enter the default the password and continue with the setup process.

2. Install the following plugins

Navigate to: `Manage Jenkins` -> `Plugins` -> `Available Plugins`

- GitHub Plugin
- SSH Pipeline Steps

### Jenkins DockerHub Credentials

1. Navigate to: `Manage Jenkins` -> `Credentials`
2. Click on `global` under the domain
3. Click on `Add Credentials`
4. Select:
    - Kind – Secret Text
    - Scope – Global
    - Secret – Your DockerHub Access Token
    - ID – DockerHub
5. Click `Create` to save the credentials

### Jenkins SSH Keypair Credentials

Note: The deploy stage in the Jenkins pipeline will SSH into an existing VM and copy over the `docker-compose.yml` file to deploy the infrastructure.

1. Navigate to: `Manage Jenkins` -> `Credentials`
2. Click on `global` under the domain
3. Click on `Add Credentials`
4. Select:
    - Kind – SSH Username with Private Key
    - Scope – Global
    - ID – deployment_vm
    - Username: The username on the VM to use
    - Private Key: Create an SSH keypair on the deployment VM and copy over the private key file contents
5. Click `Create` to save the credentials

### Jenkins Global Pipeline Configuration

The global pipeline configuration is where the shared library is configured

1. Go to Manage Jenkins -> System. Then scroll down to the "Global Pipeline Libraries" section.

2. Click "Add"

3. Enter the following details:
    - "Name": Can be set to anything 
    - "Source Code Management": Git
    - "Project Repository": The GitHub link to the repository
    - "Library Path": The location where the pipeline configuration file (Groovy File) is stored

4. Click "Save"

### Jenkins Pipeline Creation

1. Create a new item and enter the following details:
    - Set the name of the item (anything)
    - Select "Pipeline"

2. Click "Ok"

3. Enter the following details:
    - Select "GitHub Project"
        - "Project URL": The GiHub link to the project
    - Select "GitHub hook trigger for GITScm polling"
    - Select "Pipeline Script from SCM"
        - "SCM": Git
            - "Repository URL": Same as the GitHub project URL
        - "Branch Specifier": */main
        "Script Path": The location of where the Jenkinsfile is

4. Click "Save"

### GitHub Webhook 

1. Go to the GitHub project repo

2. Go to Settings -> Webhooks

3. Click "Add Webhook"

4. Enter the following details:
    - "Payload URL": The home link for the jenkins dashboard + "/github-webhook/"
    - "Content-Type": "application/json"

5. Click "Add Webhook"
