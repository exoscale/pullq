build: cljs clj

clean:
	lein clean

clj:
	lein uberjar

cljs:
	lein cljsbuild once min

docker: cljs clj
	docker build .

test:
	lein test
