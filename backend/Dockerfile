FROM node:22-slim
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install --omit=dev
COPY store.js server.js updates.js ./
COPY scripts ./scripts
RUN chmod +x /app/scripts/publish-update.sh
ENV SAVE_MONEY_DATA=/data
EXPOSE 8080
CMD ["node", "server.js"]
