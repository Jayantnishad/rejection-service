#!/bin/bash

echo "Load Testing Rejection as a Service"

# Test 1: Basic throughput
echo "Test 1: 1000 requests, 10 concurrent"
if ! ab -n 1000 -c 10 http://localhost:8080/api/v1/rejection; then
    echo "ERROR: Test 1 failed" >&2
    exit 1
fi

# Test 2: Rate limiting validation
echo "Test 2: Rate limit test - 200 requests, 20 concurrent"
if ! ab -n 200 -c 20 http://localhost:8080/api/v1/rejection; then
    echo "ERROR: Test 2 failed" >&2
    exit 1
fi

# Test 3: Sustained load
echo "Test 3: 5000 requests, 50 concurrent"
if ! ab -n 5000 -c 50 http://localhost:8080/api/v1/rejection; then
    echo "ERROR: Test 3 failed" >&2
    exit 1
fi

echo "Load testing completed"