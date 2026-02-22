package com.olorin.claudette.ui.screens.agents

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olorin.claudette.models.AgentTreeNode
import com.olorin.claudette.services.impl.AgentActivityParser
import com.olorin.claudette.ui.theme.ClaudetteBackground
import com.olorin.claudette.ui.theme.ClaudetteOnSurface
import com.olorin.claudette.ui.theme.ClaudetteOutline
import com.olorin.claudette.ui.theme.ClaudettePrimary
import com.olorin.claudette.ui.theme.ClaudettePrimaryLight
import com.olorin.claudette.ui.theme.ClaudetteSurface

private const val CHILD_INDENT_DP = 24
private const val PULSE_DURATION_MS = 1200

@Composable
fun AgentVisualizerScreen(agentParser: AgentActivityParser) {
    val rootNodes = agentParser.rootNodes
    val activeCount = agentParser.activeAgentCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with active count
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = null,
                    tint = ClaudettePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Agent Activity",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (activeCount > 0) ClaudettePrimary else ClaudetteOutline
            ) {
                Text(
                    text = "Active: $activeCount",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (rootNodes.isEmpty()) {
            EmptyAgentState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = rootNodes.toList(),
                    key = { it.id }
                ) { node ->
                    AgentNodeView(node = node, depth = 0)
                }
            }
        }
    }
}

@Composable
private fun EmptyAgentState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Memory,
                contentDescription = null,
                tint = ClaudetteOutline,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Agents Detected",
                color = ClaudetteOnSurface,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Agent activity will appear here when Claude spawns subagents",
                color = ClaudetteOutline,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun AgentNodeView(node: AgentTreeNode, depth: Int) {
    val indentDp: Dp = (depth * CHILD_INDENT_DP).dp

    Column(
        modifier = Modifier.padding(start = indentDp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = ClaudetteSurface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator
                AgentStatusIndicator(isCompleted = node.isCompleted)

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.agentType,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (node.description.isNotBlank()) {
                        Text(
                            text = node.description,
                            color = ClaudetteOnSurface,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = node.displayDuration,
                    color = ClaudetteOutline,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Render children recursively
        if (node.children.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            node.children.forEach { child ->
                AgentNodeView(node = child, depth = depth + 1)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun AgentStatusIndicator(isCompleted: Boolean) {
    if (isCompleted) {
        Surface(
            shape = CircleShape,
            color = ClaudettePrimary.copy(alpha = 0.2f),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = ClaudettePrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "agentPulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = PULSE_DURATION_MS,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

        Surface(
            shape = CircleShape,
            color = ClaudettePrimaryLight.copy(alpha = 0.2f),
            modifier = Modifier
                .size(28.dp)
                .alpha(alpha)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = CircleShape,
                    color = ClaudettePrimaryLight,
                    modifier = Modifier.size(10.dp)
                ) {}
            }
        }
    }
}
