package codes.side.color

// This color, for a typed view of [space]: it must be in [space] already.
internal fun ColorValue.requireIn(space: ColorSpace): ColorValue {
    require(this.space == space) { "$this is not in ${space.id}" }
    return this
}
