FROM openjdk:8-jre

VOLUME /tmp

ADD  Bot-7.2.1-30.jar bot.jar

RUN bash -c 'touch /bot.jar'

# install curl
RUN apt-get -y -qq update && \
	apt-get install -y -qq curl && \
	apt-get clean

# install jq to parse json within bash scripts
RUN curl -L -o /usr/local/bin/jq https://github.com/stedolan/jq/releases/download/jq-1.6/jq-linux64 && chmod +x /usr/local/bin/jq


ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/bot.jar"]
