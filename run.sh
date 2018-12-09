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
  4,4
elif [ "common" == $1 ]; then
  java -Djava.ext.dirs=./lib/ -cp classes/ StaxDemo \
  `date +%s`CBS_CLT \
  `date +%s`合法 \
  2 \
  $1 \
  GB2312 \
  origin/CBS_CLT/format.xml \
  origin/CBS_CLT/service.xml \
  4,4
elif [ "8583" == $1 ]; then
  java -Djava.ext.dirs=./lib/ -cp classes/ StaxDemo \
  `date +%s`YJN8583 \
  `date +%s`YJN8583 \
  2 \
  $1 \
  GB2312 \
  origin/8583.csv \
  None \
  None
fi


