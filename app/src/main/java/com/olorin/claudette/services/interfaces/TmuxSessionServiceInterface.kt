package com.olorin.claudette.services.interfaces

interface TmuxSessionServiceInterface {
    fun sessionName(profileId: String, prefix: String): String
    fun checkTmuxCommand(): String
    fun hasSessionCommand(sessionName: String): String
    fun attachCommand(sessionName: String): String
    fun newSessionCommand(sessionName: String, directory: String, initialCommand: String): String
    fun attachOrCreateCommand(sessionName: String, directory: String, initialCommand: String): String
}
