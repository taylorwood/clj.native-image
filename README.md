# clj.native-image

Build [GraalVM](https://www.graalvm.org) native images using [Clojure Deps and CLI tools](https://clojure.org/guides/deps_and_cli).

This should be useful for creating lightweight, native CLI executables using Clojure and `deps.edn`.

## Prerequisites

- [Clojure CLI tools](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)
- [GraalVM](https://www.graalvm.org/downloads/)

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
           {:extra-deps
            {clj.native-image
             {:git/url "https://github.com/taylorwood/clj.native-image.git"
              :sha "dc6d6e97d0f2cbaaab014531e728edb72e89171f"}}}}
 :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
```

Where `core.clj` is a class with `-main` entrypoint, for example:
```clojure
(ns core
  (:gen-class))
(defn -main [& args]
  (println "Hello, World!"))
```

From your project directory, invoke `clojure` with the `native-image` alias, specifying the main namespace (`core` in example above):
```
➜ clojure -A:native-image -m clj.native-image core
Loading core
Compiling core
Building native image 'core' with classpath 'classes:src:etc.'

   classlist:   1,944.26 ms
   8<----------------------
     [total]:  38,970.37 ms
```
Note: Either `GRAALVM_HOME` environment variable must be set, or GraalVM's `native-image` path must be passed as an argument,
and any [additional arguments](https://www.graalvm.org/docs/reference-manual/aot-compilation/#image-generation-options) will be passed to `native-image`:
```
➜ clojure -A:native-image -m clj.native-image core \
    $GRAALVM_HOME/bin/native-image --verbose
```

You can now execute the native image:
```
➜ ./core
Hello, World!
```

## Notes

The `--no-server` flag is passed to `native-image` by default, to avoid creating orphaned build servers.

## References

[GraalVM Native Image AOT Compilation](https://www.graalvm.org/docs/reference-manual/aot-compilation/)

This project was inspired by [depstar](https://github.com/healthfinch/depstar).

## Contributing

You'll need Clojure CLI tooling and GraalVM installed to test locally. Just change the source of the `clj.native-image` dependency to a `:local/root` instead of `:git/url`.

Issues, PRs, and suggestions are welcome!

## License

Copyright © 2018 Taylor Wood.

Distributed under the MIT License.