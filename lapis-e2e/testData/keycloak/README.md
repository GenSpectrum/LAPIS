# Keycloak Test Configuration

Minimal Keycloak realm configuration for testing OAuth integration in LAPIS.
Do not use this configuration in production.
It is insecure and just enough for short-lived Keycloak instances for testing.

**Realm:** `test-realm`  
**Client:** `lapis-client` (public client)  
**User:** `test-user` / `test123`

Use

```shell
./get-token.sh
```

to obtain a JWT token for testing.
