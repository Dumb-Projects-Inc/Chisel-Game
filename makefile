.PHONY: generate

generate:
	rm -rf generated/*
	sbt run
