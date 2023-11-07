FROM openjdk:8

LABEL MAINTAINER="Marvin Raiser <marvin@raiser.dev>"
LABEL Description="This image is used to run the ONE simulator lastest version"

RUN useradd one

WORKDIR /home/one/the-one

RUN apt-get update && apt-get install dos2unix libxext6 libxrender1 libxtst6 libxi6 -y

COPY . .

# Convert every file to unix line endings to prevent errors on windows
RUN find . -type f -print0 | xargs -0 dos2unix

RUN  ./compile.sh && chown -R one:one /home/one

ENTRYPOINT [ "/bin/sh", "one.sh" ]