
.PHONY: hardlint softlint test

hardlint:
	activator "; scalastyle; scapegoat; compile"

softlint:
	activator scalastyle
