# clj.native-image

Build [GraalVM](https://www.graalvm.org) native images using [Clojure Deps and CLI tools](https://clojure.org/guides/deps_and_cli).

This should be useful for creating lightweight, native CLI executables using Clojure and `deps.edn`.
See [clj.native-cli](https://github.com/taylorwood/clj.native-cli) for a starter project template.

_This project depends on tools.deps.alpha and should be considered alpha itself._

## Prerequisites

- [Clojure CLI tools](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)
- [GraalVM](https://www.graalvm.org/downloads/)

## Usage

Assuming a project structure like this (or see [example](https://github.com/taylorwood/lein-native-image/tree/master/examples/jdnsmith)):
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
                        "-H:Name=json2edn"]
            :extra-deps
            {clj.native-image
             {:git/url "https://github.com/taylorwood/clj.native-image.git"
              :sha "0f113d46f9f0d07e8a29545c636431ddf5360a7d"}}}}
 :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
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

## Notes

The `--no-server` flag is passed to `native-image` by default, to avoid creating orphaned build servers.

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