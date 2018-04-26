# JSON Validation Service

Json-Validator is a RESTful server to save JSON schemas and test JSON documents
against those schemas. This uses [json-schema-validator][3] to validate schemas
and generate error messages for invalid documents.

[3]: https://github.com/java-json-tools/json-schema-validator

This was created for the [Snowplow Internship Task][0].

[0]: https://gist.github.com/BenFradet/c73e73353085144f4d7517ae627261d5

## Building and Running

JSON Validation Service uses [sbt][1] to build and test. Before running, a
config file containing the port number and directory to store schemas must
be setup.

[1]: https://www.scala-sbt.org/

```
$ cp config.example.json config.json
$ mkdir schemas
$ sbt run
```

This will automatically download dependencies, build and run with default
options. The default config will put schemas in the `schemas` directory
in the current directory (**which must exist, otherwise schemas will not
properly be stored**) and run on port `8000`.

## Usage

Here's how to store and validate the JSON from the spec using curl.

```
$ curl -X POST -d @config-schema.json localhost:8000/schema/config-schema
{
  "action" : "uploadSchema",
  "id" : "config-schema",
  "status" : "success"
}

$ curl -X POST -d @config.json localhost:8000/validate/config-schema
{
  "action" : "validateDocument",
  "id" : "config-schema",
  "status" : "success"
}
```

If you try to validate an invalid schema, it will fail.

```
$ curl -X POST -d "{}" localhost:8000/validate/config-schema
{
  "action" : "validateDocument",
  "id" : "config-schema",
  "status" : "error",
  "message" : "error: object has missing required properties ([\"destination\",\"source\"])\n    level: \"error\"\n    schema: {\"loadingURI\":\"#\",\"pointer\":\"\"}\n    instance: {\"pointer\":\"\"}\n    domain: \"validation\"\n    keyword: \"required\"\n    required: [\"destination\",\"source\"]\n    missing: [\"destination\",\"source\"]\n\n"
}
```

Here's what the `message` field looks like by itself.

```
error: object has missing required properties (["destination","source"])
    level: "error"
    schema: {"loadingURI":"#","pointer":""}
    instance: {"pointer":""}
    domain: "validation"
    keyword: "required"
    required: ["destination","source"]
    missing:     ["destination","source"]
```

Other requests will be handled according to the spec, and appropriate HTTP
status codes will be returned will be returned with valid JSON if something
fails. All schema IDs must match the regex `^[A-Za-z0-9_-]{1,}$`.

## Testing

Tests can be run with `sbt test`, but there aren't very many.


## Known Issues

The following warning is produced by sbt when dependencies are downloaded:

```
[warn] Found version conflict(s) in library dependencies; some are suspected to be binary incompatible:
[warn]
[warn]  * com.google.code.findbugs:jsr305:3.0.1 is selected over 2.0.1                                 
[warn]      +- com.github.java-json-tools:json-schema-validator:2.2.8 (depends on 3.0.1)               
[warn]      +- com.github.fge:btf:1.2                             (depends on 2.0.1)                   
[warn]      +- com.github.fge:uri-template:0.9                    (depends on 2.0.1)
[warn]      +- com.github.java-json-tools:json-schema-core:1.2.8  (depends on 2.0.1)
[warn]      +- com.github.fge:msg-simple:1.1                      (depends on 2.0.1)
[warn]      +- com.github.fge:jackson-coreutils:1.8               (depends on 2.0.1)
[warn] Run 'evicted' to see detailed eviction warnings                                                              
```

This hasn't realized itself as an issue while running, and likely isn't causing any
problems, but should probably be resolved anyway.
