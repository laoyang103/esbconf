#!/bin/bash

rm -f classes/*.class
javac -encoding utf-8 -Djava.ext.dirs=./lib  -d classes `find ./src/ -name *.java`

if [ "xml" == $1 ]; then
  java -Djava.ext.dirs=./lib/ -cp classes/ StaxDemo \
  `date +%s`LCSA_CLT \
  `date +%s`理财 \
  2 \
  $1 \
  GB2312 \
  origin/LCSA/Format.xml\;origin/LCSA/LCSA_CLT/Format.xml \
  origin/LCSA/LCSA_CLT/Service.xml \
  4,4 \
  utf-8
elif [ "common" == $1 ]; then
  java -Djava.ext.dirs=./lib/ -cp classes/ StaxDemo \
  `date +%s`CBS_CLT \
  `date +%s`合法 \
  2 \
  $1 \
  GB2312 \
  origin/CBS/Format.xml \
  origin/CBS/CBS_CLT/Service.xml \
  6,4 \
  utf-8
elif [ "8583" == $1 ]; then
  java -Djava.ext.dirs=./lib/ -cp classes/ StaxDemo \
  `date +%s`ACQS_CLT \
  `date +%s`ACQS_CLT \
  2 \
  $1 \
  GB2312 \
  origin/ACQS/Format.xml\;origin/ACQS/ACQS_CLT/Format.xml \
  origin/ACQS/ACQS_CLT/Service.xml \
  4,4 \
  utf-8
fi


