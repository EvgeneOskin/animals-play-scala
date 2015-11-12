
.PHONY: lint

lint:
	activator "; scalastyle; scapegoat; compile"
