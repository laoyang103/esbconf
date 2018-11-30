rm classes/*.class
javac -encoding utf-8 -Djava.ext.dirs=./lib  -d classes `find ./src/ -name *.java`
java -Djava.ext.dirs=./lib/ -cp classes/ StaxDemo XmlName stdprocode XmlName stdprocode
