# KE - Experimental External Authentication for APITable Community Edition

**Note: This service is not production-ready and should be treated as an experiment.**

This fork based on `branch v0.21.0-rc.1` of [APITable](https://github.com/apitable/apitable) and tested with [Keycloak v21.1.1](https://github.com/keycloak/keycloak/releases/tag/21.1.1)

KE (short for Keycloak Experiment) is a custom fork of APITable Community Edition, designed to add external authentication capabilities. This experiment aims to integrate external authentication using the OIDC (OpenID Connect) protocol with APITable, leveraging the Keycloak authentication provider.

## License

KE is distributed under the AGPLv3 (GNU Affero General Public License version 3) license.

## Environment Variables

The following environment variables are used by KE for configuring the external authentication functionality. These variables should be set according to your specific requirements:

| Name                           | Default                                              | Description                                                                                   |
|--------------------------------|------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| OIDC_URI_GRANT_TYPE_PASSWORD   | http://localhost:8080/realms/myrealm/protocol/openid-connect/token | (Deprecated) This variable enables authorization of users over the Keycloak authentication provider using the grant_type: password. This feature is intended for testing purposes. |
| OIDC_IMPLICIT_SUCCESS_REDIRECT | ../../workbench                                      | After a successful login, the user will be redirected to the specified path.                     |
| OIDC_IMPLICIT_JWKS_URI         | http://localhost:8080/realms/myrealm/protocol/openid-connect/certs | The URL of the JWKS (JSON Web Key Set) used for validating tokens. If left empty, token validation will be disabled. |
| OIDC_IMPLICIT_IS_ENABLED       | true                                                 | Set this variable to enable OIDC  authentication.                 |
| OIDC_IMPLICIT_URL              | http://localhost:8080/realms/myrealm/protocol/openid-connect/auth | The authentication URL provided by the authentication provider.                        |
| OIDC_IMPLICIT_REDIRECT_URL     | http://localhost/api/v1/oidccallback                 | The callback URL that must match the one specified in the authentication provider.    |
| OIDC_IMPLICIT_CLIENT_ID        | login-app                                            | The client ID obtained from the authentication provider.                                |
| OIDC_IMPLICIT_RESPONSE_TYPE    | token                                                | The response type for the authentication flow.                                                 |
| OIDC_IMPLICIT_SCOPE            | email                                                | The scope of the authentication request.                                                       |

Support only `response_mode=form_post`, `fragment` and `query` is not supported.

### Tips
- how to get all of this endpoints: `curl http://keycloak.local:8080/realms/<myrealm>/.well-known/openid-configuration | jq .`
- how to build only backend: `make _build-java` and `make _build-docker-backend-server`
- how to build only web room/ws: `_build-web`

### Docker images
`rzrbld/web-server:test`
`rzrbld/backend-server:test`

## Getting Started

To use KE, follow these steps:

1. Clone this repository.
2. Set the required environment variables based on your Keycloak configuration.
3. Build and run like described in APITable readme + you need [run](https://www.keycloak.org/getting-started/getting-started-docker) and [set-up realm](https://www.baeldung.com/spring-boot-keycloak) Keycloak
4. Access APITable using the external authentication provider.

Please note that this is an experimental project and should not be used in a production environment without thorough testing and security review.

## License

KE is distributed under the AGPLv3 license. Please see the [LICENSE](LICENSE) file for more information.

## Disclaimer

This experiment is provided "as is" without warranty of any kind. Use at your own risk.
