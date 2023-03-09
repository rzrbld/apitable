# Prerequisites

* A instance of PolarDB for MySQL 8.0.
* A host with [docker](https://docs.docker.com/engine/install/) and [docker-compose v2](https://docs.docker.com/engine/install/) installed.
* 4 CPUs/8GB RAM or more are recommended.

# Modify PolarDB MySQL Credentials in .env

Example:

```text
MYSQL_HOST=polardb-hackathon.mysql.polardb.rds.aliyuncs.com
MYSQL_PORT=3306
MYSQL_DATABASE=apitable
MYSQL_USERNAME=apitable
MYSQL_PASSWORD=EYkZEjUuoYj1BdbDk0s3z48wnTu1uFzd
```

# Pull images from dockerhub and start apitable

```bash
sudo docker compose pull
sudo docker compose up -d
```

# Access the url and sign up with new account

The default port is 80.

# Happy using APITable
