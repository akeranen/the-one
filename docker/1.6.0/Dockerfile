FROM java:8
MAINTAINER Mael Auzias <the-one@mael.auzias.net>
LABEL Description="This image is used to run the ONE simulator version 1.6.0"

RUN mkdir /home/one \
  && useradd one

RUN  curl -sL https://codeload.github.com/akeranen/the-one/legacy.tar.gz/master \
   | tar xvz \
  && mv akeranen-the-one-* /home/one/the-one_1.6.0 \
  && cd /home/one/the-one_1.6.0 \
  && ./compile.sh \
  && chown -R one:one /home/one

WORKDIR /home/one/the-one_1.6.0
USER one

ENTRYPOINT ["one.sh"]
