/*
 * Copyright 2021 LINE Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linecorp.lich.thrift.logging

import android.content.Context
import android.util.Log
import com.android.dx.DexMaker
import com.android.dx.FieldId
import com.android.dx.MethodId
import com.android.dx.TypeId
import com.linecorp.lich.thrift.logging.ThriftLoggerInjector.Constants.injectedLoggerType
import com.linecorp.lich.thrift.logging.ThriftLoggerInjector.Constants.logReceiveMethod
import com.linecorp.lich.thrift.logging.ThriftLoggerInjector.Constants.logSendMethod
import com.linecorp.lich.thrift.logging.ThriftLoggerInjector.Constants.loggerStaticFieldName
import com.linecorp.lich.thrift.logging.ThriftLoggerInjector.Constants.tBaseType
import com.linecorp.lich.thrift.logging.ThriftLoggerInjector.Constants.tProtocolType
import org.apache.thrift.TBase
import org.apache.thrift.TServiceClient
import org.apache.thrift.protocol.TProtocol
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier

/**
 * Generates subclasses of Thrift Clients that log invocations of `send_*` / `recv_*` methods to
 * the given [ThriftLogger].
 */
internal class ThriftLoggerInjector(context: Context, private val logger: ThriftLogger) {

    private val generatedConstructors: MutableMap<Class<*>, Constructor<*>> = hashMapOf()

    private val codeCacheDir: File by lazy {
        File(context.codeCacheDir, "thrift_logger").also { it.mkdirs() }
    }

    /**
     * Creates a new instance of [T] that uses the same [TProtocol]s of the [client] and is injected
     * [ThriftLogger].
     */
    fun <T : TServiceClient> injectLogger(client: T): T = try {
        getInjectedConstructor(client.javaClass)
            .newInstance(client.inputProtocol, client.outputProtocol)
    } catch (e: Exception) {
        Log.w("ThriftLoggerInjector", "Failed to inject logger.", e)
        codeCacheDir.listFiles()?.forEach { it.delete() }
        client
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : TServiceClient> getInjectedConstructor(clazz: Class<T>): Constructor<out T> =
        synchronized(generatedConstructors) {
            generatedConstructors[clazz]
                ?: generateClass(clazz).also { generatedConstructors[clazz] = it }
        } as Constructor<out T>

    /**
     * Generates a subclass of [clazz] that overrides `sendBase()`, `sendBaseOneway()` and `receiveBase`.
     * These methods call [InjectedLogger] that is set as a static field of the generated class.
     *
     * @param T The type of the Thrift Client. (e.g. `FooService.Client`.)
     * @param G A subtype of [T].
     * @return The constructor of the generated class.
     */
    private fun <T : TServiceClient, G : T> generateClass(clazz: Class<T>): Constructor<G> {
        val originalClassName = clazz.name
        val generatedClassName = originalClassName + "_Logging"
        val generatedType = TypeId.get<G>("L${generatedClassName.replace('.', '/')};")
        val superType = TypeId.get(clazz)

        val dexMaker = DexMaker()

        dexMaker.declare(generatedType, "", Modifier.PUBLIC, superType)

        declareConstructor(dexMaker, generatedType, superType)

        val loggerStaticField = declareLoggerStaticField(dexMaker, generatedType)

        declareSendMethod(dexMaker, generatedType, superType, loggerStaticField, "sendBase")
        declareSendMethod(dexMaker, generatedType, superType, loggerStaticField, "sendBaseOneway")
        declareReceiveMethod(dexMaker, generatedType, superType, loggerStaticField, "receiveBase")

        val dexClassLoader = dexMaker.generateAndLoad(clazz.classLoader, codeCacheDir)

        @Suppress("UNCHECKED_CAST")
        val generatedClass = Class.forName(generatedClassName, true, dexClassLoader) as Class<G>

        initLoggerStaticField(generatedClass, originalClassName)

        return generatedClass.getConstructor(TProtocol::class.java, TProtocol::class.java)
    }

    private fun <T : TServiceClient, G : T> declareConstructor(
        dexMaker: DexMaker,
        generatedType: TypeId<G>,
        superType: TypeId<T>
    ) {
        val constructor = generatedType.getConstructor(tProtocolType, tProtocolType)
        val superConstructor = superType.getConstructor(tProtocolType, tProtocolType)
        dexMaker.declare(constructor, Modifier.PUBLIC).apply {
            val thisRef = getThis(generatedType)
            val param0 = getParameter(0, tProtocolType)
            val param1 = getParameter(1, tProtocolType)
            invokeDirect(superConstructor, null, thisRef, param0, param1)
            returnVoid()
        }
    }

    private fun <T : TServiceClient, G : T> declareSendMethod(
        dexMaker: DexMaker,
        generatedType: TypeId<G>,
        superType: TypeId<T>,
        loggerStaticField: FieldId<G, InjectedLogger>,
        methodName: String
    ) {
        val method = generatedType.getMethod(TypeId.VOID, methodName, TypeId.STRING, tBaseType)
        val superMethod = superType.getMethod(TypeId.VOID, methodName, TypeId.STRING, tBaseType)
        dexMaker.declare(method, Modifier.PUBLIC).apply {
            val injectedLogger = newLocal(injectedLoggerType)
            sget(loggerStaticField, injectedLogger)
            val param0 = getParameter(0, TypeId.STRING)
            val param1 = getParameter(1, tBaseType)
            invokeVirtual(logSendMethod, null, injectedLogger, param0, param1)
            val thisRef = getThis(generatedType)
            invokeSuper(superMethod, null, thisRef, param0, param1)
            returnVoid()
        }
    }

    @Suppress("SameParameterValue")
    private fun <T : TServiceClient, G : T> declareReceiveMethod(
        dexMaker: DexMaker,
        generatedType: TypeId<G>,
        superType: TypeId<T>,
        loggerStaticField: FieldId<G, InjectedLogger>,
        methodName: String
    ) {
        val method = generatedType.getMethod(TypeId.VOID, methodName, tBaseType, TypeId.STRING)
        val superMethod = superType.getMethod(TypeId.VOID, methodName, tBaseType, TypeId.STRING)
        dexMaker.declare(method, Modifier.PUBLIC).apply {
            val injectedLogger = newLocal(injectedLoggerType)
            val thisRef = getThis(generatedType)
            val param0 = getParameter(0, tBaseType)
            val param1 = getParameter(1, TypeId.STRING)
            invokeSuper(superMethod, null, thisRef, param0, param1)
            sget(loggerStaticField, injectedLogger)
            invokeVirtual(logReceiveMethod, null, injectedLogger, param0, param1)
            returnVoid()
        }
    }

    private fun <T : TServiceClient, G : T> declareLoggerStaticField(
        dexMaker: DexMaker,
        generatedType: TypeId<G>
    ): FieldId<G, InjectedLogger> {
        val loggerStaticField = generatedType.getField(injectedLoggerType, loggerStaticFieldName)
        dexMaker.declare(loggerStaticField, Modifier.PUBLIC or Modifier.STATIC, null)
        return loggerStaticField
    }

    private fun <T : TServiceClient, G : T> initLoggerStaticField(
        generatedClass: Class<G>,
        originalClassName: String
    ) {
        val packageName = originalClassName.substringBeforeLast('.', "")
        val serviceName = originalClassName.substringAfterLast('.').removeSuffix("\$Client")
        generatedClass.getDeclaredField(loggerStaticFieldName)
            .set(null, InjectedLogger(packageName, serviceName, logger))
    }

    private object Constants {
        const val loggerStaticFieldName: String = "\$__logger"
        val tProtocolType: TypeId<TProtocol> = TypeId.get(TProtocol::class.java)
        val tBaseType: TypeId<TBase<*, *>> = TypeId.get(TBase::class.java)
        val injectedLoggerType: TypeId<InjectedLogger> = TypeId.get(InjectedLogger::class.java)
        val logSendMethod: MethodId<InjectedLogger, Void> =
            injectedLoggerType.getMethod(TypeId.VOID, "logSend", TypeId.STRING, tBaseType)
        val logReceiveMethod: MethodId<InjectedLogger, Void> =
            injectedLoggerType.getMethod(TypeId.VOID, "logReceive", tBaseType, TypeId.STRING)
    }
}
