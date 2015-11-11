
.PHONY: lint

lint:
	activator scapegoat
	activator scalastyle
	activator compile
