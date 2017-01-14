@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.async

import java.io.Closeable
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

interface EventLoop {
	companion object {
		var _impl: EventLoop? = null
		var impl: EventLoop
			set(value) {
				_impl = value
			}
			get() {
				if (_impl == null) _impl = eventLoopDefaultImpl
				return _impl!!
			}

		fun main(eventLoop: EventLoop, entry: suspend () -> Unit): Unit {
			impl = eventLoop
			main(entry)
		}

		fun main(entry: suspend () -> Unit): Unit {
			tasksInProgress.incrementAndGet()
			impl.init()
			impl.setImmediate {
				entry.startCoroutine(object : Continuation<Unit> {
					override fun resume(value: Unit) {
						tasksInProgress.decrementAndGet()
					}

					override fun resumeWithException(exception: Throwable) {
						tasksInProgress.decrementAndGet()
						exception.printStackTrace()
					}
				})
			}
		}

		fun queue(handler: () -> Unit): Unit = impl.setImmediate(handler)
		fun setImmediate(handler: () -> Unit): Unit = impl.setImmediate(handler)
		fun setTimeout(ms: Int, callback: () -> Unit): Closeable = impl.setTimeout(ms, callback)
		fun setInterval(ms: Int, callback: () -> Unit): Closeable = impl.setInterval(ms, callback)

		suspend fun sleep(ms: Int): Unit = suspendCoroutine { c -> setTimeout(ms) { c.resume(Unit) } }
	}

	val available: Boolean
	val priority: Int
	fun init(): Unit
	fun setInterval(ms: Int, callback: () -> Unit): Closeable
	fun setTimeout(ms: Int, callback: () -> Unit): Closeable
	fun setImmediate(handler: () -> Unit): Unit
}
