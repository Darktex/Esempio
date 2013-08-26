JAVACP=src:lib/algs4-package.jar:lib/jsoup-1.7.1.jar:lib/stdlib-package.jar

src/%.class: src/%.java
	javac -cp $(JAVACP) $^

all: src/*/*.class
	mkdir -p bin
	find src -name "*.class" -exec dirname {} \; | sort | uniq | sed "s/^src/bin/g" | xargs mkdir -p
	find src -name "*.class" -exec echo {} {} \; | sed "s/ src/ bin/g" | xargs -n 2 mv

clean:
	/bin/rm -r bin
	mkdir -p bin
