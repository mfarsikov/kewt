package com.github.mfarsikov.kewt.processor

import java.io.PrintWriter
import java.io.StringWriter
import javax.annotation.processing.Messager
import javax.tools.Diagnostic

class Logger {
    companion object {
         var messager: Messager? = null
         var logLevel:LogLevel = LogLevel.INFO

        fun info(s: Any) {
            if (logLevel.ordinal <= LogLevel.INFO.ordinal) messager?.printMessage(Diagnostic.Kind.OTHER, "$s\r")
        }

        fun debug(s: Any) {
            if (logLevel.ordinal <= LogLevel.DEBUG.ordinal) messager?.printMessage(Diagnostic.Kind.NOTE, "$s\r")
        }

        fun trace(s: Any) {
            if (logLevel.ordinal <= LogLevel.TRACE.ordinal) messager?.printMessage(Diagnostic.Kind.NOTE, "$s\r")
        }

        fun warn(s: Any) {
            if (logLevel.ordinal <= LogLevel.WARN.ordinal) messager?.printMessage(Diagnostic.Kind.WARNING, "$s\r")
        }

        fun error(s: String) {
            if (logLevel.ordinal <= LogLevel.ERROR.ordinal) messager?.printMessage(Diagnostic.Kind.ERROR, "$s\r")
        }

        fun error(ex: Throwable, s: String) {

            val exception = ex.message + " " +
                    StringWriter().also {
                        ex.printStackTrace(PrintWriter(it))
                    }.toString() + "\r"

            if (logLevel.ordinal <= LogLevel.ERROR.ordinal) messager?.printMessage(Diagnostic.Kind.ERROR, "$s\r$exception")
        }
    }

    enum class LogLevel { TRACE, DEBUG, INFO, WARN, ERROR }
}