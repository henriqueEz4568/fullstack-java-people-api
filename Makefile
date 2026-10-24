.PHONY: docker\:db\:up docker\:db\:down docker\:db\:reset

docker\:db\:up:
	docker compose up -d --wait db

docker\:db\:down:
	docker compose down

docker\:db\:reset:
	docker compose down -v
	docker compose up -d --wait db
