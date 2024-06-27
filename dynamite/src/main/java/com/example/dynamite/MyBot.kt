package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move
import com.softwire.dynamite.game.Round
import java.io.File
import kotlin.math.floor
import kotlin.math.max

class MyBot : Bot {
    override fun makeMove(gamestate: Gamestate): Move {
        // Are you debugging?
        // Put a breakpoint in this method to see when we make a move
        val randomNumberBetween0And3 =
            floor(Math.random() * 3.0).toInt()
        val possibleMoves = arrayOf(Move.R, Move.P, Move.S)
        val randomMove = possibleMoves[randomNumberBetween0And3]

        val dynamitesPlayed = numberOfDynamitesPlayed(gamestate)
        val (scoreP1, scoreP2) = calculateScores(gamestate)
        val movesUntilEnd = 1000 - max(scoreP1, scoreP2)
//        println("$scoreP1 $scoreP2 $movesUntilEnd")
        val dynamiteProbability = (100.0 - dynamitesPlayed) / movesUntilEnd

        if (movesUntilEnd == 1) {
            logGamestate(gamestate)
        }

        val dynamiteRNG = Math.random()
//        println("$dynamiteRNG $dynamiteProbability")
        if (dynamiteRNG < dynamiteProbability) {
            return Move.D
        }

        return randomMove
    }

    private fun logGamestate(gamestate: Gamestate) {
        File("gamestateLog.txt").printWriter().use { out ->
            gamestate.rounds.forEach {
                out.println("${moveToText(it.p1)}-${moveToText(it.p2)} ${calculateWinner(it.p1, it.p2)}")
            }
        }
    }

    private fun moveToText(move: Move): String {
        return when (move) {
            Move.R -> "R"
            Move.P -> "P"
            Move.S -> "S"
            Move.D -> "D"
            Move.W -> "W"
        }
    }

    private fun numberOfDynamitesPlayed(gamestate: Gamestate): Int {
        var dynamites = 0
        for (round in gamestate.rounds) {
            if (round.p1 == Move.D) {
                ++dynamites
            }
        }
        return dynamites
    }

    private fun calculateScores(gamestate: Gamestate): Pair<Int, Int> {
        var scoreP1 = 0
        var scoreP2 = 0
        var scoreRolledOver = 1
        for (round in gamestate.rounds) {
            when (calculateWinner(round.p1, round.p2)) {
                1 -> {
                    scoreP1 += scoreRolledOver
                    scoreRolledOver = 1
                }
                2 -> {
                    scoreP2 += scoreRolledOver
                    scoreRolledOver = 1
                }
                0 -> scoreRolledOver++
            }
        }
        return Pair(scoreP1, scoreP2)
    }

    private fun calculateWinner(move1: Move, move2: Move): Int {
        if (move1 == move2) return 0 // Draw
        when (move1) {
            Move.D -> {
                if (move2 == Move.W) return 2
                return 1
            }
            Move.W -> {
                if (move2 == Move.D) return 1
                return 2
            }
            Move.R -> {
                if (move2 == Move.S || move2 == Move.W) return 1
                return 2
            }
            Move.P -> {
                if (move2 == Move.R || move2 == Move.W) return 1
                return 2
            }
            Move.S -> {
                if (move2 == Move.P || move2 == Move.W) return 1
                return 2
            }
        }
    }

    init {
        // Are you debugging?
        // Put a breakpoint on the line below to see when we start a new match
        println("Started new match")
    }
}