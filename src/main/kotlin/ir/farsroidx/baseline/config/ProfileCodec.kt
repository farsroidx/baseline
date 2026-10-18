package ir.farsroidx.baseline.config

import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException

/**
 * Encodes portable Boolean preferences independently of IntelliJ's project-state XML.
 *
 * Schema version 1 uses stable state-property names. Missing split preferences inherit legacy
 * shared values; other missing preferences use their defaults. Unknown names, duplicate entries
 * and non-Boolean values are rejected. Parsing
 * prohibits document types and external resources. No project or UI state is modified.
 */
internal class ProfileCodec {

    /**
     * Returns deterministic UTF-8 XML containing every preference in catalog order.
     *
     * @param options preferences to export, including unapplied form values
     * @return portable profile text with its schema and plugin version
     */
    fun encode(options: FormatOptions): String {

        return buildString {

            append( XML_HEADER )

            append( "<" + ROOT + " " + FORMAT_VERSION + "=\"" + SCHEMA_VERSION + "\" " )

            append( PLUGIN_VERSION + "=\"" + RELEASE_VERSION + "\">\n" )

            for (option in FormatOption.entries) {

                append( "    <" + OPTION + " " + NAME + "=\"" + option.property.name + "\" " )

                append( VALUE + "=\"" + option.property.get( options ) + "\"/>\n" )

            }

            append( "</" + ROOT + ">\n" )

        }
    }

    /**
     * Imports existing profiles while ignoring the retired generated-source preference.
     *
     * Every entry still receives duplicate-name and Boolean validation. Other unknown names
     * remain invalid. No project state changes until the caller applies the returned options.
     *
     * @param text bounded portable XML profile
     * @return validated formatting preferences
     * @throws IllegalArgumentException for malformed or unsupported entries
     *
     * @author Mohammad ali Riazati
     * @since 1.0.1
     */
    fun decode(text: String): FormatOptions {

        require( text.toByteArray( Charsets.UTF_8 ).size <= MAX_PROFILE_BYTES ) {

            "The settings file is too large."

        }

        val factory = DocumentBuilderFactory.newInstance()

        factory.setFeature( XMLConstants.FEATURE_SECURE_PROCESSING, true )

        factory.setFeature( DISALLOW_DOCTYPE, true )

        factory.setFeature( EXTERNAL_GENERAL_ENTITIES, false )

        factory.setFeature( EXTERNAL_PARAMETER_ENTITIES, false )

        factory.setAttribute( XMLConstants.ACCESS_EXTERNAL_DTD, "" )

        factory.setAttribute( XMLConstants.ACCESS_EXTERNAL_SCHEMA, "" )

        factory.isXIncludeAware = false

        factory.isExpandEntityReferences = false

        val parser = factory.newDocumentBuilder()

        parser.setErrorHandler(
            object : ErrorHandler {

            /** Rejects parser warnings without printing them to the IDE error stream. */
            override fun warning(exception: SAXParseException) {

                throw exception

            }

            /** Propagates malformed XML errors to the profile validation boundary. */
            override fun error(exception: SAXParseException) {

                throw exception

            }

            /** Propagates fatal XML errors without continuing with partial data. */
            override fun fatalError(exception: SAXParseException) {

                throw exception

            }
        }
        )

        val document = try {

            parser.parse( InputSource( StringReader( text ) ) )

        } catch (exception: org.xml.sax.SAXException) {

            throw IllegalArgumentException("The file does not contain valid Baseline settings XML.", exception)

        }

        val root = document.documentElement

        require( root.tagName == ROOT && root.getAttribute( FORMAT_VERSION ) == SCHEMA_VERSION ) {

            "The file is not a supported Baseline settings profile."

        }

        val options = FormatOptions()

        val catalog = FormatOption.entries.associateBy { it.property.name }

        val imported = mutableSetOf<String>()

        for (index in 0 until root.childNodes.length) {

            val node = root.childNodes.item( index )

            if (
                node.nodeType == org.w3c.dom.Node.COMMENT_NODE ||
                node.nodeType == org.w3c.dom.Node.TEXT_NODE && node.textContent.isBlank()
            ) {

                continue
            }

            require( node is Element && node.tagName == OPTION && !node.hasChildNodes() ) {

                "The settings profile contains an unsupported entry."

            }

            val name = node.getAttribute( NAME )

            val option = catalog[name]

            require( (option != null || name == RETIRED_GENERATED_OPTION) && imported.add( name ) ) {

                "Unknown or repeated preference: " + name

            }

            val value = node.getAttribute( VALUE )
                .toBooleanStrictOrNull()

            require( value != null ) {

                "Preference " + name + " must be true or false."

            }

            option?.property?.set( options, value )

        }

        if (FormatOptions::padMultipleArguments.name !in imported) {

            options.padMultipleArguments = options.padSingleArguments

        }

        for (property in listOf(
            FormatOptions::formatParameterAnnotations,
            FormatOptions::formatFieldAnnotations,
            FormatOptions::formatClassAnnotations
        )) {

            if (property.name !in imported) {

                property.set( options, options.formatAnnotations )

            }

        }

        return options

    }

    companion object {

        /** Upper bound for portable settings files; ordinary profiles are only a few kilobytes. */
        const val MAX_PROFILE_BYTES = 64 * 1024

        private const val XML_HEADER                  = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        private const val EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities"
        private const val EXTERNAL_GENERAL_ENTITIES   = "http://xml.org/sax/features/external-general-entities"
        private const val DISALLOW_DOCTYPE            = "http://apache.org/xml/features/disallow-doctype-decl"
        private const val RETIRED_GENERATED_OPTION = "showGeneratedSources"

        private const val RELEASE_VERSION             = "1.0.2"
        private const val FORMAT_VERSION              = "formatVersion"
        private const val SCHEMA_VERSION              = "1"
        private const val PLUGIN_VERSION              = "pluginVersion"
        private const val OPTION                      = "option"
        private const val VALUE                       = "value"
        private const val ROOT                        = "baseline-settings"
        private const val NAME                        = "name"

    }
}
