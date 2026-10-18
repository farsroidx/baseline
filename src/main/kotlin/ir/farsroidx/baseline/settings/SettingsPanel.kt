package ir.farsroidx.baseline.settings

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Rectangle
import javax.swing.JPanel
import javax.swing.Scrollable

/** Fits the Settings viewport while child option lists and the preview editor scroll independently. */
internal class SettingsPanel : JPanel( BorderLayout() ), Scrollable {

    /** Returns a bounded initial size independent of the preview document's length and line widths. */
    override fun getPreferredScrollableViewportSize(): Dimension {

        return Dimension( INITIAL_WIDTH, INITIAL_HEIGHT )

    }

    /** Keeps the entire Settings page inside the available horizontal viewport. */
    override fun getScrollableTracksViewportWidth(): Boolean {

        return true

    }

    /** Keeps the header and action buttons visible while children handle their own vertical scrolling. */
    override fun getScrollableTracksViewportHeight(): Boolean {

        return true

    }

    /** Supplies a fallback wheel increment for hosts that wrap this component in a scroll pane. */
    override fun getScrollableUnitIncrement(visibleRect: Rectangle, orientation: Int, direction: Int): Int {

        return SCROLL_INCREMENT

    }

    /** Supplies a bounded fallback page increment without scrolling the entire settings form. */
    override fun getScrollableBlockIncrement(visibleRect: Rectangle, orientation: Int, direction: Int): Int {

        return SCROLL_INCREMENT

    }

    companion object {

        private const val INITIAL_WIDTH = 900
        private const val INITIAL_HEIGHT = 600
        private const val SCROLL_INCREMENT = 20

    }

}