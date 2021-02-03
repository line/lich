# Lich

**Lich** is a library collection that enhances the development of Android apps.

All the libraries are available on JCenter.
```groovy
repositories {
    jcenter()
}
```

## Libraries

- [component](component) - Lightweight framework for managing singleton components on Android apps.
- [viewmodel](viewmodel) - Lightweight framework for managing ViewModels.
- [lifecycle](lifecycle) - A small library for Android Jetpack Lifecycle.
- [okhttp](okhttp) - Coroutine-aware extensions for OkHttp.
- [thrift](thrift) - Coroutine-aware extensions for Apache Thrift.

## Sample App

- [sample_app](sample_app) - A sample application using Lich libraries.
- [sample_feature](sample_feature) - A sample feature module using Lich component library.
- [dagger_sample_app](dagger_sample_app) - A sample application to demonstrate integration of Lich and [Dagger2](https://dagger.dev/).
- [dagger_sample_feature](dagger_sample_feature) - A sample feature module for `dagger_sample_app`.

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
