# clj.native-image

Build [GraalVM](https://www.graalvm.org) native images using [Clojure Deps and CLI tools](https://clojure.org/guides/deps_and_cli).

This should be useful for creating lightweight, native CLI executables using Clojure and `deps.edn`.
See [clj.native-cli](https://github.com/taylorwood/clj.native-cli) for a starter project template.

_This project depends on tools.deps.alpha and should be considered alpha itself._

## Prerequisites

- [Clojure CLI tools](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)
- [GraalVM](https://www.graalvm.org/downloads/)

  **NOTE:** As of GraalVM 19.0.0, `native-image` is no longer included by default:
  > Native Image was extracted from the base GraalVM distribution. Currently it is available as an early adopter plugin. To install it, run: `gu install native-image`. After this additional step, the `native-image` executable will be in the `bin` directory, as for the previous releases.

  ```
  ➜ $GRAALVM_HOME/bin/gu install native-image
  Downloading: Component catalog from www.graalvm.org
  Processing component archive: Native Image
  Downloading: Component native-image: Native Image  from github.com
  Installing new component: Native Image licence files (org.graalvm.native-image, version 19.0.0)
  ```

## Usage

Assuming a project structure like this:
```
.
├── deps.edn
└── src
    └── core.clj
```

In your `deps.edn` specify an alias with a dependency on `clj.native-image`:
```clojure
{:aliases {:native-image
           {:main-opts ["-m clj.native-image core"
                        "--initialize-at-build-time"
                        "-H:Name=core"]
            :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
            :extra-deps
            {clj.native-image
             {:git/url "https://github.com/taylorwood/clj.native-image.git"
              :sha "567176ddb0f7507c8b0969e0a10f60f848afaf7d"}}}}}
```

Where `core.clj` is a class with `-main` entrypoint, for example:
```clojure
(ns core
  (:gen-class))
(defn -main [& args]
  (println "Hello, World!"))
```

From your project directory, invoke `clojure` with the `native-image` alias, specifying the main namespace
(`core` in example above):
```
➜ clojure -A:native-image
Loading core
Compiling core
Building native image 'core' with classpath 'classes:src:etc.'

   classlist:   1,944.26 ms
   8<----------------------
     [total]:  38,970.37 ms
```
Note: Either `GRAALVM_HOME` environment variable must be set, or GraalVM's `native-image` path must be passed as an argument,
and any [additional arguments](https://www.graalvm.org/docs/reference-manual/aot-compilation/#image-generation-options)
will be passed to `native-image` e.g.:
```
➜ clojure -A:native-image --verbose
```

You can now execute the native image:
```
➜ ./core
Hello, World!
```

See [this Gist](https://gist.github.com/taylorwood/23d370f70b8b09dbf6d31cd4f27d31ff) for another example.

### Example Projects

There are example deps.edn projects in the [lein-native-image](https://github.com/taylorwood/lein-native-image) repo:
- [jdnsmith](https://github.com/taylorwood/lein-native-image/blob/master/examples/http-api) - CLI JSON-to-EDN transformer
- [http-api](https://github.com/taylorwood/lein-native-image/blob/master/examples/http-api) - simple HTTP API server
- [clojurl](https://github.com/taylorwood/clojurl) - cURL-like tool using clojure.spec, HTTPS, hiccup

## Caveats

The `--no-server` flag is passed to `native-image` by default, to avoid creating orphaned build servers.

Also see [caveats](https://github.com/taylorwood/lein-native-image#caveats) section of lein-native-image.

## References

[GraalVM Native Image AOT Compilation](https://www.graalvm.org/docs/reference-manual/aot-compilation/)

This project was inspired by [depstar](https://github.com/healthfinch/depstar).

## Contributing

You'll need Clojure CLI tooling and GraalVM installed to test locally.
Just change the source of the `clj.native-image` dependency to a `:local/root` instead of `:git/url`.

Issues, PRs, and suggestions are welcome!

## License

Copyright © 2018 Taylor Wood.

Distributed under the MIT License.
