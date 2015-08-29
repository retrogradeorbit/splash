server:
	cd resources/public && python -m SimpleHTTPServer

upload:
	rsync -av resources/public/ www-data@atomsk.procrustes.net:~/www.procrustes.net/public_html/

archive:
	cd resources/public && tar cvzf ../../www.procrustes.net.tar.gz .

images: resources/public/img/sprites.png

sfx:
	cd src/sfx && oggenc *.wav

	-mkdir resources/public/sfx/
	cp src/sfx/*.ogg resources/public/sfx/
