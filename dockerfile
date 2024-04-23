FROM jenkins/jenkins:latest

LABEL maintainer="sschlegel1@my.bcit.ca"

USER root

RUN mkdir /var/log/jenkins
RUN mkdir /var/cache/jenkins

RUN chown -R jenkins:jenkins /var/log/jenkins
RUN chown -R jenkins:jenkins /var/cache/jenkins

RUN apt-get update
RUN apt-get install -y python3 python3-pip
RUN apt-get install -y pylint
RUN apt-get install -y zip
RUN apt-get install -y pkg-config

RUN apt install -y maven
RUN apt install -y python3.11-venv
RUN apt install -y python3-dev default-libmysqlclient-dev

RUN python3 -m venv /opt/venv
RUN chmod -R 777 /opt/venv

ENV PATH="/opt/venv/bin:$PATH"

RUN pip install --upgrade pip --break-system-packages
RUN pip install coverage --break-system-packages
RUN pip install SQLAlchemy --break-system-packages
RUN pip install safety --break-system-packages

RUN apt-get update && \
apt-get -y install apt-transport-https \
    ca-certificates \
    curl \
    gnupg2 \
    software-properties-common && \
    curl -fsSL https://download.docker.com/linux/$(. /etc/os-release; \
    echo "$ID")/gpg > /tmp/dkey; apt-key add /tmp/dkey && \
    add-apt-repository \
    "deb [arch=amd64] https://download.docker.com/linux/$(. /etc/os-release; echo "$ID") \
    $(lsb_release -cs) \
    stable" && \
apt-get update && \
apt-get -y install docker-ce

RUN apt-get install -y docker-ce
RUN usermod -aG docker jenkins
RUN newgrp docker

RUN curl -sL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs

USER jenkins

ENV JAVA_OPTS="-Xmx4096m"
ENV JENKINS_OPTS="--logfile=/var/log/jenkins/jenkins.log --webroot=/var/cache/jenkins/war"