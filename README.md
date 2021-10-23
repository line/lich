# Lich

[ ![CI](https://github.com/line/lich/workflows/CI/badge.svg?branch=master&event=push) ](https://github.com/line/lich/actions?query=workflow%3ACI+branch%3Amaster+event%3Apush)

**Lich** is a library collection that enhances the development of Android apps.

All the libraries are available on [Maven Central](https://search.maven.org/search?q=g:com.linecorp.lich).

## Libraries

- [component](component) - Lightweight framework for managing singleton components on Android apps.
- [viewmodel](viewmodel) - Lightweight framework for managing ViewModels in the same way as "Lich Component".
- [savedstate](savedstate) - A library that provides type-safe access to [saved instance state](https://developer.android.com/topic/libraries/architecture/viewmodel-savedstate).
- [lifecycle](lifecycle) - A small library for Android Jetpack Lifecycle.
- [okhttp](okhttp) - Coroutine-aware extensions for OkHttp.
- [thrift](thrift) - A library for using Apache Thrift in combination with OkHttp.

## Sample App

- [sample_app](sample_app) - A sample application using Lich libraries.
- [sample_feature](sample_feature) - A sample feature module using Lich component library.

## Material

- (In Japanese) https://speakerdeck.com/line_developers/kotlin-lich

## License

```text
Copyright 2019 LINE Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

See [LICENSE](LICENSE) for more detail.
