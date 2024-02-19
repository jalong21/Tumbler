# Use OpenJDK 11 as the base image
FROM mcr.microsoft.com/openjdk/jdk:11-ubuntu

# Install Scala and sbt
# be sure to check java and scala version compatibilities
ARG SCALA_VERSION=2.13.5
ARG SBT_VERSION=1.4.9

RUN apt-get update
RUN apt-get install -y curl gnupg2
RUN apt-get install apt-transport-https curl gnupg -yqq
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
RUN apt install gpg-agent
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import
RUN chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg
RUN apt-get update
# -y means answer yes to install questions
RUN apt-get install sbt -y


# Copy the application code into the container
WORKDIR /app
COPY . /app

# Compile the application
RUN sbt clean compile

# Run the application
# Note: Replace `your-app-main-class` with the main class of your application and adjust the pdort if needed
CMD ["sbt", "run"]

# Expose the application port
EXPOSE 9000
