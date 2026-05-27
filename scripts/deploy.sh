#!/bin/bash
set -e
echo "Building backend..."
cd backend && mvn clean package -DskipTests && cd ..
echo "Building Docker image..."
docker build -t booking-server:latest -f deploy/docker/Dockerfile.backend .
echo "Pushing to registry..."
# docker tag booking-server:latest your-registry/booking-server:latest
# docker push your-registry/booking-server:latest
echo "Applying K8s configs..."
kubectl apply -f deploy/k8s/namespace.yaml
kubectl apply -f deploy/k8s/configmap.yaml
kubectl apply -f deploy/k8s/secret.yaml
kubectl apply -f deploy/k8s/deployment-backend.yaml
kubectl apply -f deploy/k8s/service-backend.yaml
kubectl apply -f deploy/k8s/hpa.yaml
echo "Deploy complete!"
