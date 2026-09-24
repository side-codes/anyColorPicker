package codes.side.color

/**
 * Required to subclass [ColorSpace] directly.
 *
 * [ColorSpace] may gain abstract members in a minor release, which a subclass compiled against the
 * earlier one would not implement. The factories on [ColorSpace.Companion] build spaces that stay
 * compatible; subclass only when no factory fits, and expect to recompile.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
public annotation class ExperimentalColorSpaceApi
