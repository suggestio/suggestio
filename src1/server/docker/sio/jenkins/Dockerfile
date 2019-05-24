## https://wiki.archlinux.org/index.php/Jenkins
## https://github.com/jenkinsci/docker/blob/master/Dockerfile
##
## Start:
## docker volume create jenkins_home
## docker run -p 8090:8090 -p 50000:50000 -v jenkins_home:/var/lib/jenkins sio/jenkins-sio

FROM docker-registry.suggest.io/sio/sio2/os-base-www
LABEL maintainer="konstantin.nikiforov@cbca.ru"

ARG user=jenkins
ARG group=jenkins
ARG uid=32463
ARG gid=$uid
ARG http_port=8080
## TODO Забить этот же порт в /etc/conf.d/jenkins
ARG agent_port=50000
ARG JENKINS_HOME=/var/lib/jenkins

# Jenkins is run with user `jenkins`, uid = 1000
# If you bind mount a volume from the host or a data container,
# ensure you use the same uid
RUN mkdir -p $JENKINS_HOME && \
  chown ${uid}:${gid} $JENKINS_HOME && \
  groupadd -g ${gid} ${group} && \
  useradd --system -d "$JENKINS_HOME" -u ${uid} -g ${gid} -m -s /bin/bash ${user}

RUN $PACMAN -Sy --noconfirm jenkins git npm nodejs yarn sbt

# Jenkins home directory is a volume, so configuration and build history
# can be persisted and survive image upgrades
VOLUME $JENKINS_HOME

# `/usr/share/java/jenkins/ref/` contains all reference configuration we want
# to set on a fresh new installation. Use it to bundle additional plugins
# or config file with your custom jenkins Docker image.
RUN mkdir -p /usr/share/java/jenkins/ref/init.groovy.d

RUN chown -R ${user} "$JENKINS_HOME" /usr/share/java/jenkins/ref

RUN sio-finalize-container.sh

# jenkins version being bundled in this docker image
ARG JENKINS_VERSION
ENV JENKINS_VERSION=${JENKINS_VERSION:-2.121.1} \
    JENKINS_UC=https://updates.jenkins.io \
    JENKINS_UC_EXPERIMENTAL=https://updates.jenkins.io/experimental \
    JENKINS_INCREMENTALS_REPO_MIRROR=https://repo.jenkins-ci.org/incrementals \
    JENKINS_HOME=$JENKINS_HOME \
    COPY_REFERENCE_FILE_LOG=$JENKINS_HOME/copy_reference_file.log \
    JENKINS_PORT=${http_port} \
    JENKINS_SLAVE_AGENT_PORT=${agent_port}

# for main web interface:
EXPOSE ${http_port}/tcp

# will be used by attached slave agents:
EXPOSE ${agent_port}/tcp

USER ${user}

COPY jenkins-support /usr/local/bin/jenkins-support
COPY jenkins.sh /usr/local/bin/jenkins.sh

ENTRYPOINT ["/sbin/tini", "--", "/usr/local/bin/jenkins.sh"]

# from a derived Dockerfile, can use `RUN plugins.sh active.txt` to setup /usr/share/java/jenkins/ref/plugins from a support bundle
COPY plugins.sh /usr/local/bin/plugins.sh
COPY install-plugins.sh /usr/local/bin/install-plugins.sh

