package ir.farsroidx.baseline.model

/**
 * Identifies a declaration group for field ordering and alignment.
 * @param access visibility rank
 * @param type declared or safely inferred type name
 */
internal data class FieldGroup(val access: Int, val type: String)