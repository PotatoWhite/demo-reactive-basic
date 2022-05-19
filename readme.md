```shell
❯ docker run -d -p 6379:6379 --name local_redis redis
❯ docker run -d -p 5432:5432 -e POSTGRES_USER=root -e POSTGRES_PASSWORD='potato' --name local_postgres postgres
```

```shell
❯ docker exec -it local_postgres psql -U root
psql (14.2 (Debian 14.2-1.pgdg110+1))
Type "help" for help.

root=# create user potato password 'test1234' superuser;
CREATE ROLE
root=# create database reactive_basic owner potato;
CREATE DATABASE
root=# 
```

```shell
❯ docker exec -it local_postgres psql -U potato reactive_basic
psql (14.2 (Debian 14.2-1.pgdg110+1))
Type "help" for help.

reactive_basic=# create table public.users(id SERIAL PRIMARY KEY, name VARCHAR(255), email VARCHAR(255));
CREATE TABLE
reactive_basic=# 
```