# Multi-stage build for ReleaseGuard Frontend

# Stage 1: Build static bundle
FROM node:22-alpine AS builder
WORKDIR /app

# Install dependencies with clean cache
COPY package*.json ./
RUN npm ci

# Copy sources and compile TypeScript + Vite production bundle
COPY . .
RUN npm run build

# Stage 2: Production web server
FROM nginx:1.27-alpine AS runtime

# Copy custom reverse proxy and SPA Nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled static assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q --spider http://localhost/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
