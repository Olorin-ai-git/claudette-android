package com.olorin.claudette.services.impl

import com.olorin.claudette.services.interfaces.TmuxSessionServiceInterface

class TmuxSessionService : TmuxSessionServiceInterface {

    override fun sessionName(profileId: String, prefix: String): String {
        val shortId = profileId.take(8).lowercase()
        return "$prefix-$shortId"
    }

    override fun checkTmuxCommand(): String = "which tmux"

    override fun hasSessionCommand(sessionName: String): String =
        "tmux has-session -t ${shellEscape(sessionName)} 2>/dev/null"

    override fun attachCommand(sessionName: String): String =
        "tmux attach-session -t ${shellEscape(sessionName)}"

    override fun newSessionCommand(sessionName: String, directory: String, initialCommand: String): String =
        "tmux new-session -s ${shellEscape(sessionName)}" +
            " -c ${shellEscape(directory)}" +
            " ${shellEscape(initialCommand)}"

    override fun attachOrCreateCommand(sessionName: String, directory: String, initialCommand: String): String {
        val escaped = shellEscape(sessionName)
        val escapedCmd = shellEscape(initialCommand)

        // When an existing tmux session is found, check if claude is still
        // running inside it. If the process has exited, send the launch command
        // into the pane so it restarts with --continue before we attach.
        val attachWithRestart =
            "tmux list-panes -t $escaped -F '#{pane_current_command}' | grep -q claude || " +
                "tmux send-keys -t $escaped $escapedCmd Enter; " +
                attachCommand(sessionName)

        val tmuxBranch =
            "tmux has-session -t $escaped 2>/dev/null && { $attachWithRestart; } || " +
                newSessionCommand(sessionName, directory, initialCommand)

        val directFallback = "cd ${shellEscape(directory)} && exec $escapedCmd"

        // If tmux is not installed fall back to a plain cd + exec
        return "(command -v tmux >/dev/null 2>&1 && ($tmuxBranch)) || ($directFallback)"
    }

    private fun shellEscape(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"
}
