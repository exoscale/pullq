pullq: Pull request queue visualization
========================================

![travis build](https://api.travis-ci.org/exoscale/pullq.svg?branch=master)

This is a tool inspired by [review
gator](https://github.com/fginther/review-gator) with a number of
differences:

- Github-only support
- A departure in the presentation style
- Decoupling of stats gathering and presentation
- Additional dynamic filters (authors, repositories, labels, status)

![screenshot](https://i.imgur.com/grOsAsw.png)

## tl;dr

To get from nothing to a working pullq assuming you have a
[clojure](https://clojure.org) environment already setup:

```
git clone https://github.com/exoscale/pullq.git
cd pullq
cat > pullq.conf <<EOF
exoscale pullq 1
exoscale cli 2
EOF
lein run
xdg-open http://localhost:8000
```

## Running in Docker

An included Dockerfile lets you conveniently build a ready-to-go pullq container
with:

```
make docker
```

**Note**: the top-level directory `pullq.conf` file will be included in the
resulting container image. You can of course mount a configuration file at the
same path instead.

You can easily pass your GITHUB_TOKEN to the container when starting it, and
redirect the default port to localhost (as an example) with:

```
docker -it -e GITHUB_TOKEN=... -p 127.0.0.1:8000:8000 <image hash>
```

## Overview

Pullq is a [Clojure](https://clojure.org) server that will serve a static
[ClojureScript](https://clojurescript.org/) application, and periodically
refresh its knowledge of the configured project's pull request status.

## Running pullq

Since a pre-built javascript application is included in this repo,
all you need to use pullq are:

- Creating a valid configuration file
- Running the server

You will need to install [Leiningen](https://leiningen.org/), the
[Clojure](https://clojure.org) build tool. It will take care of gathering other
dependencies for you (putting them in your ~/.m2 folder).

### Creating a configuration file

To run the server, you will need a config file, defaulting to
`pullq.conf` in the current directory. You will also need to provide a github
token for private repository access, either in the `GITHUB_TOKEN` environment
variable, or by providing the `-t` command line flag.

```
# An example "pullq.conf" file
# user repo minimum-oks
exoscale pullq 1
clojure clojure 2
clojure clojurescript 2
```

### Running the server

In its simplest form, Running the server is done by running

```
lein run
```

at the top-level directory of this code's checkout.

However, it is recommended to generate a [github personal access
token](https://github.com/settings/tokens) for this app to use, in order to have
a more reasonable rate-limiting against the github API. The invocation becomes;

```
GITHUB_TOKEN=... lein run
```

You can now browse to http://localhost:8000, have fun!

## Generating the Javascript application

This is only needed if you wish to modify the frontend (the clojurescript
application).

You will need [leiningen](http://leiningen.org) installed for this step as well.

In the repository, run: `lein cljsbuild once min`

Note that the compiled application in `resources/` is updated.

## Development Mode

### Start Cider from Emacs:

Put this in your Emacs config file:

```
(setq cider-cljs-lein-repl
	"(do (require 'figwheel-sidecar.repl-api)
         (figwheel-sidecar.repl-api/start-figwheel!)
         (figwheel-sidecar.repl-api/cljs-repl))")
```

Navigate to a clojurescript file and start a figwheel REPL with
`cider-jack-in-clojurescript` or (`C-c M-J`)

### Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.
Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Caveats

This is extracted from internal tooling at
[Exoscale](https://exoscale.com) and might be a bit rough around the
edges.
