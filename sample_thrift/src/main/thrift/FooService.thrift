namespace java com.linecorp.lich.sample.thrift

typedef i64 FooId

struct FooParam {
    1: i32 number = 0
    2: optional string comment
}

struct FooResponse {
    1: i32 number = 0
    2: string message
    3: optional string comment
}

exception FooException {
    1: string reason
}

service FooService {

    void ping(),

    FooResponse callFoo(1:FooId id, 2:string name, 3:FooParam param) throws (1:FooException e)
}
