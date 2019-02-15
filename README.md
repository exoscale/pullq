pullq: Pull request queue visualization
========================================

This is a tool inspired by [review
gator](https://github.com/fginther/review-gator) with a number of
differences:

- Github-only support
- A departure in the presentation style
- Decoupling of stats gathering and presentation
- Additional dynamic filters (authors, repositories, labels, status)

![screenshot](https://i.imgur.com/MgAi4YR.png)

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
cd build
python3 -m http.server
xdg-open http://localhost:8000
```

You can now run this in e.g a cron:

```
lein run
```

## Overview

Pullq is essentially a clojure program that generates a data file, that you run
periodically (via cron, or [jenkins](https://jenkins.io), or whatever you like).

The web application included in this repository can be served along with that
data file with any webserver as static files, and will render an interface like
the screenshot above.

## Running pullq

Since a pre-built javascript application is included in this repo,
all you need to use pullq are these two steps:

- Generate a data-file for your repositories
- Look at the output

### Generating the data-file

You will need to install [Leiningen](https://leiningen.org/), the
[Clojure](https://clojure.org) build tool. It will take care of gathering other
dependencies for you (putting them in your ~/.m2 folder).

To generate the data file, you will need a config file, defaulting to
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

You can then generate the data periodically with:

```
lein run
```

### Looking at the output

The `build` subdirectory can now be served with any webserver as static files.

As an example, let's serve it with python3's built-in webserver:

```
(cd build  && python3 -m http.server)
```

You can now browse to http://localhost:8000, have fun!

## Generating the Javascript application

This is only needed if you wish to modify the frontend (the clojurescript
application).

You will need [leiningen](http://leiningen.org) installed for this step as well.

In the repository, run: `lein cljsbuild once min`

You probably will want to move your built app.js file from
`resources/public/js/compiled` to `build/js/compiled`, so that your changes are
served with your data file etc...

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
