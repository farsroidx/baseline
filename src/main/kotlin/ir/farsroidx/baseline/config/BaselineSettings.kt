package ir.farsroidx.baseline.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Owns project-specific formatter preferences, saved in `.idea/baseline.xml`.
 *
 * Defaults remain enabled when older project state lacks a newly introduced property.
 * Formatting reads a snapshot rather than retaining this mutable persistence bean.
 */
@Service( Service.Level.PROJECT )
@State(name = "BaselineSettings", storages = [Storage( "baseline.xml" )])
class BaselineSettings : PersistentStateComponent<FormatOptions> {

    private var options = FormatOptions()

    /** Returns the live bean for platform serialization; consumers should use [snapshot]. */
    override fun getState(): FormatOptions {

        return options

    }

    /** Replaces project preferences with an independent copy of the deserialized [state]. */
    override fun loadState(state: FormatOptions) {

        options = state.snapshot()

    }

    /** Returns an isolated set of preferences for one complete formatting operation. */
    fun snapshot(): FormatOptions {

        return options.snapshot()

    }
}