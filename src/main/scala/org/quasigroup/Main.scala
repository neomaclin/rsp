package org.quasigroup

import cats.effect.std.Random
import cats.effect.{ExitCode, IO, IOApp, Ref}
import cats.syntax.flatMap.*
import org.quasigroup.core.rps.*

object Main extends IOApp:
  def run(args: List[String]): IO[ExitCode] =
    for
      given Random[IO] <- Random.scalaUtilRandom[IO]
      _ <- Ref.of[IO, Score](Score()) >>= fight[IO]
    yield ExitCode.Success
