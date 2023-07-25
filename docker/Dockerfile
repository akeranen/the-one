FROM java:8
MAINTAINER Mael Auzias <the-one@mael.auzias.net>
LABEL Description="This image is used to run the ONE simulator lastest version"

RUN mkdir /home/one \
  && useradd one

RUN  curl -sL https://codeload.github.com/akeranen/the-one/legacy.tar.gz/master \
   | tar xvz \
  && mv akeranen-the-one-* /home/one/the-one_latest \
  && cd /home/one/the-one_latest \
  && ./compile.sh \
  && chown -R one:one /home/one

WORKDIR /home/one/the-one_lastest
USER one

ENTRYPOINT ["./one.sh"]
