mud:
	javac src/MUD.java; \
	javac src/Vertex.java; \
	javac src/Edge.java; \
	javac src/Player.java; \
	javac src/Client.java; \
	javac src/MUDServer.java; \
	javac src/MUDServerMainline.java; \
	javac src/State.java; \
	javac src/ColourPrinter.java; \
	cd saves/muds; \
	rm *.msav; \
	cd ../players; \
	rm *.sav; \
	cd ../..; \

clean:
	cd src; \
	rm *.class; \
	cd ..

