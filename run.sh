rm -f classes/*.class
javac -encoding utf-8 -Djava.ext.dirs=./lib  -d classes `find ./src/ -name *.java`
java -Djava.ext.dirs=./lib/ -cp classes/ StaxDemo \
`date +%s`CBS_CLT \
`date +%s`合法 \
2 \
common \
GB2312 \
origin/CBS_CLT/format.xml \
origin/CBS_CLT/service.xml \
4,4
