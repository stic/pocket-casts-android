package au.com.shiftyjelly.pocketcasts.utils.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.combine as kotlinCombine

inline fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    crossinline transform: suspend (T1, T2, T3, T4, T5, T6) -> R,
): Flow<R> = kotlinCombine(flow1, flow2, flow3, flow4, flow5, flow6) { args: Array<*> ->
    @Suppress("UNCHECKED_CAST")
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
    )
}

inline fun <T1, T2, T3, T4, T5, T6, T7, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    crossinline transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R,
): Flow<R> = kotlinCombine(flow1, flow2, flow3, flow4, flow5, flow6, flow7) { array ->
    @Suppress("UNCHECKED_CAST")
    transform(
        array[0] as T1,
        array[1] as T2,
        array[2] as T3,
        array[3] as T4,
        array[4] as T5,
        array[5] as T6,
        array[6] as T7,
    )
}

fun <T> Flow<T>.windowed(size: Int) = flow {
    check(size > 0) { "Window size must be positive: $size" }
    val queue = ArrayDeque<T>(size)
    collect { item ->
        if (queue.size < size) {
            queue.addLast(item)
            if (queue.size == size) {
                emit(queue.toList())
            }
        } else {
            queue.removeFirst()
            queue.addLast(item)
            emit(queue.toList())
        }
    }
}
