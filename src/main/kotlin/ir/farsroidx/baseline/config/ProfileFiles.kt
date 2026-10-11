package ir.farsroidx.baseline.config

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Reads bounded UTF-8 profiles and replaces exported files using a temporary sibling file. */
internal class ProfileFiles {

    /**
     * Reads and validates a profile without changing any stored project preferences.
     *
     * @param path file selected for import
     * @return fully validated independent preferences
     * @throws java.io.IOException if the file cannot be read
     * @throws IllegalArgumentException if the file is oversized or invalid
     */
    fun read(path: Path): FormatOptions {

        require( Files.size( path ) <= ProfileCodec.MAX_PROFILE_BYTES ) {

            "The settings file is too large."

        }

        return ProfileCodec().decode( Files.readString( path, Charsets.UTF_8 ) )

    }

    /**
     * Writes a complete profile before replacing the user-selected destination.
     *
     * Uses an atomic move where supported, otherwise a regular replacement move. Temporary
     * files are removed in all cases; existing output remains intact if writing fails.
     *
     * @param path export destination selected by the user
     * @param options current form preferences to serialize
     * @throws java.io.IOException if writing, moving or temporary-file cleanup fails
     */
    fun write(path: Path, options: FormatOptions) {

        val target = path.toAbsolutePath()
            .normalize()

        val temporary = Files.createTempFile( target.parent, TEMPORARY_PREFIX, TEMPORARY_SUFFIX )

        try {

            Files.writeString( temporary, ProfileCodec().encode( options ), Charsets.UTF_8 )

            try {

                Files.move( temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING )

            } catch (exception: AtomicMoveNotSupportedException) {

                Files.move( temporary, target, StandardCopyOption.REPLACE_EXISTING )

            }
        } finally {

            Files.deleteIfExists( temporary )

        }
    }

    companion object {

        private const val TEMPORARY_PREFIX = ".baseline-profile-"
        private const val TEMPORARY_SUFFIX = ".tmp"

    }
}