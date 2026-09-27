package codes.side.colorpicker.foundation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Rasters shared by every plane that asks for one: a raster being built is awaited rather than built again,
 * and the last [kept] built are kept. Two planes asking for the same colors, as a dialog's copy measured for
 * its layout and the copy shown do, build one raster, and a switch back to the space just left builds none.
 *
 * Used from the composition's thread alone, where each plane's effect runs, so it takes no lock; [raster]'s
 * build decides where the raster itself is built.
 */
internal class PlaneRasters<T : Any>(private val kept: Int) {
    private val built = ArrayDeque<Pair<PlaneRequest, T>>()
    private val building = HashMap<PlaneRequest, CompletableDeferred<T>>()

    /** The raster for [request]: one kept, one another plane is building, or else what [build] builds. */
    suspend fun raster(request: PlaneRequest, build: suspend () -> T): T {
        while (true) {
            built.firstOrNull { it.first == request }?.let { return it.second }
            val pending = building[request] ?: break
            try {
                return pending.await()
            } catch (e: CancellationException) {
                // The plane building it went away before it was done: build it here instead, unless this
                // plane went away too.
                currentCoroutineContext().ensureActive()
            }
        }
        val pending = CompletableDeferred<T>()
        building[request] = pending
        try {
            val raster = build()
            built.addFirst(request to raster)
            if (built.size > kept) built.removeLast()
            pending.complete(raster)
            return raster
        } catch (e: Throwable) {
            pending.completeExceptionally(e)
            throw e
        } finally {
            building.remove(request)
        }
    }
}
