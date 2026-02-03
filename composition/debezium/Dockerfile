FROM quay.io/debezium/connect:3.2.0.Final

COPY ./configs /opt/custom/configs/connection
COPY ./send-connection-files.sh /opt/custom/scripts/send-connection-files.sh

USER root
RUN chown -R 1000:1000 /opt/custom/configs
RUN chmod +x /opt/custom/scripts/send-connection-files.sh
USER 1000
