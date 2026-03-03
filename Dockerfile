# Builder stage: compile edal-java and ncWMS
# Using eclipse-temurin:11-jdk-jammy as builder to keep build toolchain explicit
FROM eclipse-temurin:11-jdk-jammy AS builder

# renovate: datasource=github-tags depName=Reading-eScience-Centre/edal-java
ARG EDAL_VERSION=edal-1.5.3

RUN apt-get update && \
    apt-get install -y --no-install-recommends git maven && \
    rm -rf /var/lib/apt/lists/*

# Build edal-java from source (not yet released to Maven Central)
RUN mkdir /edal && \
    cd /edal && \
    git clone --depth 1 --branch ${EDAL_VERSION} \
    https://github.com/Reading-eScience-Centre/edal-java.git . && \
    mvn clean install -B --no-transfer-progress -DskipTests

# Build ncWMS
COPY . /ncWMS
RUN cd /ncWMS && \
    mvn clean package -B --no-transfer-progress -DskipTests && \
    mkdir /ncWMS-war && \
    unzip /ncWMS/target/ncWMS2.war -d /ncWMS-war/

# Runtime stage: official Tomcat 10.1 LTS on JRE 11
# Tomcat 10.1.x = Jakarta EE 10 / Servlet 6.0 (replaces Tomcat 8.5 EOL 2024)
FROM tomcat:10.1-jre11-temurin

LABEL maintainer="kyle@axiomdatascience.com"
LABEL org.opencontainers.image.source="https://github.com/BIZFactoryGmbH/ncwms"
LABEL org.opencontainers.image.description="ncWMS2 Web Map Service"

# Remove default Tomcat sample apps
RUN rm -rf ${CATALINA_HOME}/webapps/ROOT \
    ${CATALINA_HOME}/webapps/docs \
    ${CATALINA_HOME}/webapps/examples \
    ${CATALINA_HOME}/webapps/host-manager \
    ${CATALINA_HOME}/webapps/manager

# Deploy ncWMS WAR
COPY --from=builder /ncWMS-war/ ${CATALINA_HOME}/webapps/ncWMS2/

# Set login-config to BASIC (auth handled through Tomcat)
RUN sed -i -e 's/DIGEST/BASIC/' ${CATALINA_HOME}/webapps/ncWMS2/WEB-INF/web.xml

# Copy runtime configuration
COPY config/setenv.sh       ${CATALINA_HOME}/bin/setenv.sh
COPY config/ecache.xml      ${CATALINA_HOME}/conf/ecache.xml
COPY config/tomcat-users.xml ${CATALINA_HOME}/conf/tomcat-users.xml
RUN mkdir -p ${CATALINA_HOME}/conf/Catalina/localhost/
COPY config/ncWMS.xml       ${CATALINA_HOME}/conf/Catalina/localhost/ncWMS.xml
RUN mkdir -p ${CATALINA_HOME}/.ncWMS2
COPY config/config.xml      ${CATALINA_HOME}/.ncWMS2/config.xml

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8080 8443

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]
