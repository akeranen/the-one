FROM openjdk:8

LABEL MAINTAINER="Marvin Raiser <marvin@raiser.dev>"
LABEL Description="This image is used to run the ONE simulator lastest version"

RUN mkdir /home/one \
  && useradd one

RUN apt-get update && apt-get install libxext6 libxrender1 libxtst6 libxi6 -y

RUN  curl -sL https://codeload.github.com/akeranen/the-one/legacy.tar.gz/master \
   | tar xvz \
  && mv akeranen-the-one-* /home/one/the-one_latest \
  && cd /home/one/the-one_latest \
  && ./compile.sh \
  && chown -R one:one /home/one

WORKDIR /home/one/the-one_latest
USER one

ENTRYPOINT [ "/bin/sh", "one.sh" ]