-keepclassmembers class org.apache.thrift.TServiceClient {
    protected void sendBase(...);
    protected void sendBaseOneway(...);
    protected void receiveBase(...);
}
-keepnames class * extends org.apache.thrift.TServiceClient
-keepclassmembers class * extends org.apache.thrift.TServiceClient {
    <init>(org.apache.thrift.protocol.TProtocol, org.apache.thrift.protocol.TProtocol);
}
-keepclassmembers class com.linecorp.lich.thrift.logging.InjectedLogger {
    <methods>;
}
