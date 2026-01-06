FROM postgres:latest

COPY . /docker-entrypoint-initdb.d/

USER root
RUN chmod 755 /docker-entrypoint-initdb.d/*.sql
USER postgres