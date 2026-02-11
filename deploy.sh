#!/bin/bash

# CodeArena Docker Deployment Script
# This script deploys the entire CodeArena platform with a single command

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   CodeArena Docker Deployment         ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed. Please install Docker first.${NC}"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed. Please install Docker Compose first.${NC}"
    exit 1
fi

# Check for .env file
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠️  No .env file found. Creating from template...${NC}"
    if [ -f .env.docker ]; then
        cp .env.docker .env
        echo -e "${GREEN}✅ Created .env file from template${NC}"
        echo -e "${YELLOW}⚠️  Please edit .env file with your actual values before deploying!${NC}"
        echo ""
        echo "Required configurations:"
        echo "  - DATABASE_PASSWORD"
        echo "  - MONGO_PASSWORD"
        echo "  - REDIS_PASSWORD"
        echo "  - JWT_SECRET (generate with: openssl rand -base64 48)"
        echo "  - JUDGE0_API_KEY (if using Judge0)"
        echo ""
        read -p "Have you configured the .env file? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo -e "${RED}❌ Deployment cancelled. Please configure .env file first.${NC}"
            exit 1
        fi
    else
        echo -e "${RED}❌ .env.docker template not found!${NC}"
        exit 1
    fi
fi

# Parse command line arguments
COMMAND=${1:-up}
BUILD_FLAG=""

case "$COMMAND" in
    up)
        echo -e "${GREEN}🚀 Starting CodeArena...${NC}"
        BUILD_FLAG="--build"
        ;;
    down)
        echo -e "${YELLOW}🛑 Stopping CodeArena...${NC}"
        docker-compose down
        echo -e "${GREEN}✅ CodeArena stopped${NC}"
        exit 0
        ;;
    restart)
        echo -e "${YELLOW}🔄 Restarting CodeArena...${NC}"
        docker-compose down
        BUILD_FLAG="--build"
        ;;
    logs)
        echo -e "${GREEN}📋 Showing logs...${NC}"
        docker-compose logs -f
        exit 0
        ;;
    clean)
        echo -e "${RED}🗑️  Cleaning up (this will remove all data!)${NC}"
        read -p "Are you sure? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker-compose down -v
            echo -e "${GREEN}✅ Cleaned up successfully${NC}"
        fi
        exit 0
        ;;
    *)
        echo "Usage: $0 {up|down|restart|logs|clean}"
        echo ""
        echo "Commands:"
        echo "  up       - Start CodeArena (default)"
        echo "  down     - Stop CodeArena"
        echo "  restart  - Restart CodeArena"
        echo "  logs     - Show logs"
        echo "  clean    - Remove all containers and volumes (⚠️  destroys data)"
        exit 1
        ;;
esac

# Build and start services
echo -e "${GREEN}📦 Building images...${NC}"
docker-compose build --no-cache

echo -e "${GREEN}🚀 Starting services...${NC}"
docker-compose up -d

echo ""
echo -e "${GREEN}⏳ Waiting for services to be healthy...${NC}"

# Wait for services to be healthy
MAX_WAIT=120  # 2 minutes
ELAPSED=0
INTERVAL=5

while [ $ELAPSED -lt $MAX_WAIT ]; do
    HEALTHY=$(docker-compose ps | grep -c "healthy" || true)
    TOTAL=$(docker-compose ps | grep -c "codearena-" || true)
    
    if [ "$HEALTHY" -eq "$TOTAL" ] && [ "$TOTAL" -gt 0 ]; then
        echo -e "${GREEN}✅ All services are healthy!${NC}"
        break
    fi
    
    echo -e "${YELLOW}⏳ Waiting... ($HEALTHY/$TOTAL services healthy)${NC}"
    sleep $INTERVAL
    ELAPSED=$((ELAPSED + INTERVAL))
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
    echo -e "${RED}⚠️  Timeout waiting for services. Check logs with: $0 logs${NC}"
    docker-compose ps
    exit 1
fi

# Show service status
echo ""
echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   CodeArena is Running! 🎉            ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
echo ""
echo "📍 Service URLs:"
echo "  • Frontend:  http://localhost:$(grep FRONTEND_PORT .env | cut -d '=' -f2 || echo 80)"
echo "  • Backend:   http://localhost:$(grep BACKEND_PORT .env | cut -d '=' -f2 || echo 8080)"
echo "  • API Docs:  http://localhost:$(grep BACKEND_PORT .env | cut -d '=' -f2 || echo 8080)/swagger-ui.html"
echo "  • Health:    http://localhost:$(grep BACKEND_PORT .env | cut -d '=' -f2 || echo 8080)/actuator/health"
echo ""
echo "🔧 Useful commands:"
echo "  • View logs:       $0 logs"
echo "  • Stop services:   $0 down"
echo "  • Restart:         $0 restart"
echo "  • Clean all data:  $0 clean"
echo ""
echo "📊 Service status:"
docker-compose ps
