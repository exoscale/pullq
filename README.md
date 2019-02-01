pullq: Pull request queue visualization
========================================

This is a tool inspired by [review
gator](https://github.com/fginther/review-gator) with a number of
departures:

- Github-only support
- A departure in the presentation style
- Decoupling of stats gathering and presentation
- Additional dynamic filters (authors, repositories, labels, status)

![screenshot](https://i.imgur.com/MgAi4YR.png)

## Quickstart

A pre-built javascript application exist in the repo,
so to test this program you can take these two steps:

- Generate a data-file for your repositories
- Look at the output

### Generating the data-file

You will need [clojure](http://clojure.org) installed on your
system.

To generate the data file, you will need a config file, usually
stored in `pullq.conf`, you will also need to provide a
github token, either in the `GITHUB_TOKEN` environment variable,
or by provide the `-t` command line flag.

```
# user repo minimum-oks
org awesome-repo 2
org some-repo 1

# other repositories
my-other-org repo3 1
```

You can then generate the data with: `clojure -m pullq.main`

### Looking at the output

```
(cd build  && python -m http.server)
```

You can now browse to http://localhost:8000, have fun!

## Generating the Javascript application

You will need [leiningen](http://leiningen.org).
In the repository, run: `lein cljsbuild once min`


## Development Mode

### Start Cider from Emacs:

Put this in your Emacs config file:

```
(setq cider-cljs-lein-repl
	"(do (require 'figwheel-sidecar.repl-api)
         (figwheel-sidecar.repl-api/start-figwheel!)
         (figwheel-sidecar.repl-api/cljs-repl))")
```

Navigate to a clojurescript file and start a figwheel REPL with `cider-jack-in-clojurescript` or (`C-c M-J`)

### Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.
Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Production Build


To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```

## Caveats

This is extracted from internal tooling at
[Exoscale](https://exoscale.com) and might be a bit rough around the
edges.
