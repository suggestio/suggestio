## https://wiki.archlinux.org/index.php/Jenkins
## https://github.com/jenkinsci/docker/blob/master/Dockerfile
##
## Start:
## docker run -p 8080:8080 -p 50000:50000 sio/jenkins-sio

FROM sio/os-base-www
MAINTAINER Konstantin Nikiforov konstantin.nikiforov@cbca.ru

ARG user=jenkins
ARG group=jenkins
ARG uid=32463
ARG gid=$uid
ARG http_port=8080
ARG agent_port=50000
ARG JENKINS_HOME=/var/lib/jenkins

ENV JENKINS_HOME=$JENKINS_HOME \
    JENKINS_SLAVE_AGENT_PORT=${agent_port}

# Jenkins is run with user `jenkins`, uid = 1000
# If you bind mount a volume from the host or a data container,
# ensure you use the same uid
RUN mkdir -p $JENKINS_HOME && \
  chown ${uid}:${gid} $JENKINS_HOME && \
  groupadd -g ${gid} ${group} && \
  useradd -d "$JENKINS_HOME" -u ${uid} -g ${gid} -m -s /bin/bash ${user}

RUN $PACMAN -Sy --noconfirm jenkins git npm nodejs yarn sbt

# Jenkins home directory is a volume, so configuration and build history
# can be persisted and survive image upgrades
VOLUME $JENKINS_HOME

# `/usr/share/java/jenkins/ref/` contains all reference configuration we want
# to set on a fresh new installation. Use it to bundle additional plugins
# or config file with your custom jenkins Docker image.
RUN mkdir -p /usr/share/java/jenkins/ref/init.groovy.d

# jenkins version being bundled in this docker image
ARG JENKINS_VERSION
ENV JENKINS_VERSION=${JENKINS_VERSION:-2.121.1} \
    JENKINS_UC=https://updates.jenkins.io \
    JENKINS_UC_EXPERIMENTAL=https://updates.jenkins.io/experimental \
    JENKINS_INCREMENTALS_REPO_MIRROR=https://repo.jenkins-ci.org/incrementals \
    COPY_REFERENCE_FILE_LOG=$JENKINS_HOME/copy_reference_file.log
RUN chown -R ${user} "$JENKINS_HOME" /usr/share/java/jenkins/ref

# for main web interface:
EXPOSE ${http_port}/tcp

# will be used by attached slave agents:
EXPOSE ${agent_port}/tcp

RUN sio-finalize-container.sh

USER ${user}

COPY jenkins-support /usr/local/bin/jenkins-support
COPY jenkins.sh /usr/local/bin/jenkins.sh

ENTRYPOINT ["/sbin/tini", "--", "/usr/local/bin/jenkins.sh"]

# from a derived Dockerfile, can use `RUN plugins.sh active.txt` to setup /usr/share/java/jenkins/ref/plugins from a support bundle
COPY plugins.sh /usr/local/bin/plugins.sh
COPY install-plugins.sh /usr/local/bin/install-plugins.sh

