package ir.farsroidx.baseline.structure

internal object CascadeWidth {

    private val WHITESPACE = Regex( "\\s+" )

    private const val SPACE_WIDTH = 1

    fun measure(type: String, name: String): Int {

        return type.replace( WHITESPACE, " " )
            .trim().length + SPACE_WIDTH + name.length

    }
}