rm -f classes/*.class
javac -encoding utf-8 -Djava.ext.dirs=./lib  -d classes `find ./src/ -name *.java`
java -Djava.ext.dirs=./lib/ -cp classes/ StaxDemo \
`date +%s`ABC2_SVR \
理财`date +%s` \
2 \
xml \
GB2312 \
origin/ABC2_SVR/format.xml \
origin/ABC2_SVR/service.xml \
4,4
