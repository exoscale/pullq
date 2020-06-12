FROM clojure:openjdk-11-lein AS build
VOLUME /etc/pullq /outdir

ADD . /src

RUN cd /src && \
    lein do clean, uberjar, cljsbuild once min && \
    cp -r build /build && \
    cp resources/public/js/compiled/app.js /build/js/compiled/app.js && \
    cp target/pullq.jar /build

FROM openjdk:11 AS run

COPY --from=build /build build

# When running this will mean the environment will need to contain GITHUB_TOKEN
ENTRYPOINT java -jar /build/pullq.jar -f /etc/pullq/pullq.conf -S /outdir
