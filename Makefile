
.PHONY: hardlint softlint test install

hardlint:
	./activator "; scalastyle; scapegoat; compile"

softlint:
	./activator scalastyle

test:
	./activator clean coverage test
	./activator coverageReport

install:
	bower install
