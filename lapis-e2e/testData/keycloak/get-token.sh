#!/bin/bash

curl -s -X POST "http://localhost:8180/realms/test-realm/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=lapis-client" \
    -d "username=test-user" \
    -d "password=test123"
