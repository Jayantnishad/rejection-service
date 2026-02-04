# Docker Testing Commands

# 1. Build the Docker image
docker build -t rejection-service:test .

# 2. Run the container
docker run -d -p 8080:8080 --name rejection-test rejection-service:test

# 3. Test the application
curl http://localhost:8080/api/v1/health
curl http://localhost:8080/api/v1/rejection
curl http://localhost:8080/

# 4. Check container health
docker ps
docker logs rejection-test

# 5. Test health check
docker inspect --format='{{.State.Health.Status}}' rejection-test

# 6. Clean up
docker stop rejection-test
docker rm rejection-test
docker rmi rejection-service:test