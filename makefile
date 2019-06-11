# $Id$
# This file is part of the overall build.
.SILENT:
.SUFFIXES:

.PHONY: all clean build clean server rebuild package relnotes clean_relnotes publish

DAEMON := --no-daemon # to prevent using the Gradle Daemon in CI
SVNVERSION := $(shell svn info . | sed -n "s/Last Changed Rev: //p")
GRADLE := ./gradlew -PSVNVERSION="$(SVNVERSION)" $(DAEMON)
TOOL := TICSJenkins

all: build

build:
	$(GRADLE) jpi

clean: clean_relnotes
	$(GRADLE) clean

rebuild: clean all

server:
	$(GRADLE) server

TICSVERSION=$(shell cat ../../make/TICSVERSION)

package: build

# The SVN repository number from which revisions onwards one must
# collect release notes.
STARTREV := 28789
relnotes:
ifeq ($(OS),Windows_NT)
	svn log --xml -r $(SVNVERSION):$(STARTREV) | msxsl -o $(TOOL)-relnotes.html - svn-log.xslt
else
	svn log --xml -r $(SVNVERSION):$(STARTREV) | xsltproc -o $(TOOL)-relnotes.html svn-log.xslt -
endif

clean_relnotes:
	rm -f $(TOOL)-relnotes.html

DEST=imposter:/var/home/wilde/ticsweb/pub/plugins/jenkins

publish: package relnotes
	scp build/libs/tics.hpi $(DEST)/$(TOOL)-$(TICSVERSION).$(SVNVERSION).hpi
	scp $(TOOL)-relnotes.html $(DEST)
