# pagopa-ecommerce-payment-requests-service

## What is this?

_pagoPA - eCommerce_ microservice to retrieve _payment requests_ data or manage _carts_ (a set of _payment requests_)
with redirects to [_pagoPA – Checkout_](https://checkout.pagopa.it).

## Requirements

- **Java 21** or higher
- **Maven 3.6+**
- **Docker** (for containerized deployment)

### Environment variables

| Variable name                     | Description                                                                                         | type          | default |
|-----------------------------------|-----------------------------------------------------------------------------------------------------|---------------|---------|
| CHECKOUT_URL                      | Redirection URL for Checkout carts                                                                  | url (string)  |         |
| REDIS_HOST                        | Host where the redis instance used to persist idempotency keys can be found                         | string        |         |
| REDIS_PASSWORD                    | Password used for connecting to Redis instance                                                      | string        |         |
| REDIS_PORT                        | Port used for connecting to Redis instance                                                          | number        |         |
| REDIS_SSL_ENABLED                 | Whether SSL is enabled when connecting to Redis                                                     | boolean       |         |
| NODO_HOSTNAME                     | Nodo connection host name                                                                           | string        |         |
| NODO_PER_PSP_URI                  | Nodo per PSP URI                                                                                    | string        |         |
| NODE_FOR_PSP_URI                  | Nodo for PSP URI                                                                                    | string        |         |
| NODO_READ_TIMEOUT                 | Http read timeout for all call made to Nodo                                                         | number        |         |
| NODO_CONNECTION_TIMEOUT           | Http connection timeout for all call made to Nodo                                                   | number        |         |
| NODO_CONNECTION_STRING            | Connection string containing information used to make Nodo calls                                    | json (string) |         |
| CARTS_MAX_ALLOWED_PAYMENT_NOTICES | Max allowed number of payment notices to be processed for a POST carts request                      | number        |         |
| PERSONAL_DATA_VAULT_API_KEY       | API Key for Personal Data Vault (PDV is used to safely encrypt PIIs, e.g. the user's email address) | string        |         |
| PERSONAL_DATA_VAULT_API_BASE_PATH | API base path for Personal Data Vault                                                               | string        |         |
| NODO_NODEFORPSP_API_KEY           | API Key for NODE FOR PSP WS                                                                         | string        |         |
| NODO_NODEFORECOMMERCE_API_KEY     | API Key for Nodo checkposition API                                                                  | string        |         |
| SECURITY_API_KEY_PRIMARY          | Primary API Key used to secure payment-requests service's APIs                                      | string        |         |
| SECURITY_API_KEY_SECONDARY        | Secondary API Key used to secure payment-requests service's APIs                                    | string        |         |
| GITHUB_TOKEN                      | GitHub Personal Access Token with packages:read permission for dependencies verification            | string        |         |
An example configuration of these environment variables is in the `.env.example` file.

## Run the application with `Docker`

### Prerequisites
Set up GitHub authentication for packages (required for pagopa-ecommerce-commons dependency):

1. Configure Maven settings file:
- **If you don't have ~/.m2/settings.xml:**
```sh
cp settings.xml.template ~/.m2/settings.xml
```
- **If you already have ~/.m2/settings.xml:** Edit the file to add the GitHub server configuration from `settings.xml.template`, or replace the `${GITHUB_TOKEN}` placeholder with your actual token.


2. Set your GitHub token:
```sh
export GITHUB_TOKEN=your_github_token_with_packages_read_permission
```

**Note:** The settings.xml file is required for Maven to authenticate with GitHub Packages. Without proper configuration, builds will fail with 401 Unauthorized errors.

### Build Docker Image
```sh
docker build --secret id=GITHUB_TOKEN,env=GITHUB_TOKEN -t pagopa-ecommerce-payment-requests-service .
```

### Run with Docker Compose

Create your environment typing :

```sh
cp .env.example .env
```

Then from current project directory run :

```sh
docker-compose up

## Run the application with `springboot-plugin`

Setup your environment typing :

```sh
$ cp .env.example .env
$ export $(grep -v '^#' .env | xargs)
```

Then from the root project directory run :

```sh
$ mvn spring-boot:run
```

## Code formatting

Code formatting checks are automatically performed during build phase.
If the code is not well formatted an error is raised blocking the maven build.

Helpful commands:

```sh
mvn spotless:check # --> used to perform format checks
mvn spotless:apply # --> used to format all misformatted files
```
## CI

Repo has Github workflow and actions that trigger Azure devops deploy pipeline once a PR is merged on main branch.

In order to properly set version bump parameters for call Azure devops deploy pipelines will be check for the following
tags presence during PR analysis:

| Tag                | Semantic versioning scope | Meaning                                                           |
|--------------------|---------------------------|-------------------------------------------------------------------|
| patch              | Application version       | Patch-bump application version into pom.xml and Chart app version |
| minor              | Application version       | Minor-bump application version into pom.xml and Chart app version |
| major              | Application version       | Major-bump application version into pom.xml and Chart app version |
| ignore-for-release | Application version       | Ignore application version bump                                   |
| chart-patch        | Chart version             | Patch-bump Chart version                                          |
| chart-minor        | Chart version             | Minor-bump Chart version                                          |
| chart-major        | Chart version             | Major-bump Chart version                                          |
| skip-release       | Any                       | The release will be skipped altogether                            |

For the check to be successfully passed only one of the `Application version` labels and only ones of
the `Chart version` labels must be contemporary present for a given PR or the `skip-release` for skipping release step

--


## Dependency Verification

This project uses the [pagopa/depcheck](https://github.com/pagopa/depcheck) Maven plugin to verify SHA-256 hashes of all dependencies, ensuring supply chain integrity and preventing dependency tampering attacks.

### How It Works
The plugin maintains a JSON file containing SHA-256 hashes of all project dependencies. During verification, it compares the hashes of resolved artifacts against the stored values, failing the build if any mismatches are detected.

### Configuration

```xml
<plugin>
<groupId>it.pagopa.maven</groupId>
<artifactId>depcheck</artifactId>
<version>1.3.0</version>
<configuration>
	<fileName>dep-sha256.json</fileName>
	<includePlugins>false</includePlugins>
	<includeParent>false</includeParent>
	<excludes>
	<!-- Optional: Exclude specific dependencies -->
	</excludes>
</configuration>
<executions>
	<execution>
	<phase>validate</phase>
	<goals>
		<goal>verify</goal>
	</goals>
	</execution>
</executions>
</plugin>
```

### Usage
First of all, ensure your GitHub token and `settings.xml` are properly configured.

1. **Generate hashes**: When adding new dependencies or updating existing ones:
```sh
mvn depcheck:generate
```
**NOTE**: Always commit the updated hash file to version control after adding or updating dependencies

2. **Verify hashes**: This happens automatically during the `validate` phase, and so, automatically, in CI/CD pipelines.
You can also explicitly run:
```sh
mvn depcheck:verify
```

### Important Notes

- **Maven plugins** have empty SHA-256 values by default as they're not resolved as JAR files during the regular build. Right now `includePlugins=false` avoid empty hashes and plugin check.
