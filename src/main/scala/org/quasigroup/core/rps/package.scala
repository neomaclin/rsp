package org.quasigroup.core

import cats.derived.*
import cats.effect.Ref
import cats.effect.std.{Console, Random}
import cats.syntax.all.*
import cats.{Eq, Functor, Monad, Show}

package rps:

  enum Action derives Eq, Show:
    def losingTo(other: Action): Boolean =
      (other.ordinal + 1) % Action.values.length == this.ordinal
      // (other match
      //   case Action.Rock     => Action.Scissors
      //   case Action.Scissors => Action.Paper
      //   case Action.Paper    => Action.Rock
      // ) === this
    case Rock, Scissors, Paper

  enum ActionOutcome derives Eq, Show:
    case Win, Lose, Draw

  final case class Score(player: Int = 0, computer: Int = 0):
    private def winnerScores(result: ActionOutcome, lastScore: Int): Int =
      if result === ActionOutcome.Win then lastScore + 1 else lastScore

    def player(result: ActionOutcome): Score   =
      this.copy(player = winnerScores(result, player))
    def computer(result: ActionOutcome): Score =
      this.copy(computer = winnerScores(result, computer))

  final case class BattleResult(
      player: ActionOutcome = ActionOutcome.Draw,
      computer: ActionOutcome = ActionOutcome.Draw
  )

  def battle(action1: Action, action2: Action): BattleResult =
    if action1 === action2 then BattleResult()
    else if action1.losingTo(action2) then BattleResult(ActionOutcome.Lose, ActionOutcome.Win)
    else BattleResult(ActionOutcome.Win, ActionOutcome.Lose)

  def computerNextMove[F[_]: Random: Functor]: F[Action] =
    Random[F].nextIntBounded(Action.values.length).map(Action.fromOrdinal)

  def userNextMove[F[_]: Console: Monad]: F[Action] =
    val inputEffect =
      for
        _     <- Console[F].println("Please tell me your next move:(0-2)")
        _     <- Console[F].println("Options are: 0=Rock, 1=Scissors, 2=Paper")
        input <- Console[F].readLine
      yield
        val hasValidInput =
          Action.values.map(_.ordinal.toString).contains(input)
        Option.when(hasValidInput)(Action.fromOrdinal(input.toInt))

    Monad[F]
      .iterateWhile(inputEffect)(_.isEmpty)
      .map(_.get)

  def updateScore[F[_]: Console: Monad](
      result: BattleResult,
      score: Ref[F, Score]
  ): F[Unit] =
    for
      newScore <- score.updateAndGet(_.player(result.player).computer(result.computer))
      _        <- Console[F].println(
        s"Current Score is  user:${newScore.player}, computer: ${newScore.computer}"
      )
    yield ()

  def fight[F[_]: Monad: Console: Random](score: Ref[F, Score]): F[Unit] =
    val gameLoopEffect =
      for
        playerAction   <- userNextMove[F]
        computerAction <- computerNextMove[F]
        result         <- battle(playerAction, computerAction).pure[F]
        _              <- updateScore(result, score)
        _              <- Console[F].println(s"The result of your action is ${result.player}")
        _              <- Console[F].println(s"Do you want to go for another round?(Y/n)")
        input          <- Console[F].readLine
      yield input.toLowerCase === "n"

    Monad[F]
      .iterateUntil(gameLoopEffect)(identity)
      .void
