package codes.side.color

/**
 * Required to subclass [ColorSpace] or [TransferFunction] directly, and to construct the parts such a
 * subclass describes itself with: a [ColorChannel], a [ChannelKind.Hue] and a new [HueFamily].
 *
 * Either class may gain abstract members in a minor release, which a subclass compiled against the
 * earlier one would not implement, and those constructors may gain parameters. The factories on
 * [ColorSpace.Companion] and [TransferFunction.Companion] build instances that stay compatible, and
 * [HueFamily.Companion] holds the families they use; subclass only when no factory fits, and expect
 * to recompile.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
public annotation class ExperimentalColorSpaceApi
