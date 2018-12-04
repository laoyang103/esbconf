rm classes/*.class
javac -encoding utf-8 -Djava.ext.dirs=./lib  -d classes `find ./src/ -name *.java`
java -Djava.ext.dirs=./lib/ -cp classes/ StaxDemo \
ABC2_SVR`date +%s` \
理财`date +%s` \
2 \
xml \
GB2312 \
/opt/esbconf/origin/ABC2_SVR/format.xml \
/opt/esbconf/origin/ABC2_SVR/service.xml \
4,4
