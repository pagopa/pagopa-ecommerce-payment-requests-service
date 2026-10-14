FROM node:14-alpine@sha256:434215b487a329c9e867202ff89e704d3a75e554822e07f3e0c0f9e606121b33

RUN mkdir -p /home/node/newman/node_modules && chown -R node:node /home/node/newman
WORKDIR /home/node/newman
COPY --chown=node:node . .
RUN yarn global add newman
USER node

CMD newman run carts-api-tests.json --environment=env/carts-api-tests-env-local.json --reporters cli,junit --reporter-junit-export Results/api-TEST.xml
