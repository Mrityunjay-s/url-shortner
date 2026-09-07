.PHONY: build run

build:
	docker buildx build --platform linux/amd64 -t jayacr.azurecr.io/urlshortner:$(TAG) . --push