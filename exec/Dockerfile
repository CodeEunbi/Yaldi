# EC2 home/ubuntu/yaldi/jenkins/Dockerfile
# Jenkins용 dockerfile

FROM jenkins/jenkins:2.533-jdk21

USER root

# Node.js 24.x 설치
RUN apt-get update && \
    apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_24.x | bash - && \
    apt-get install -y nodejs && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 설치 확인
RUN node -v && npm -v

USER jenkins
