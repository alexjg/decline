package com.monovore.decline

private[decline] object Help {

  def render(parser: Command[_]): String = {

    val commands = commandList(parser.options)

    val commandUsage =
      if (commands.isEmpty) Nil else List("<command>", "[...]")

    val commandHelp =
      if (commands.isEmpty) Nil
      else s"Subcommands: ${ commands.map { _.name }.mkString(", ") }" ::
        commands.map(render)

    val optionsHelp = {
      val optionsDetail = detail(parser.options)
      if (optionsDetail.isEmpty) Nil
      else (s"Options and flags:" :: optionsDetail).mkString("\n") :: Nil
    }

    val parts = List(
      s"Usage: ${parser.name} ${(usage(parser.options) ++ args(parser.options) ++ commandUsage).mkString(" ")}",
      parser.header
    ) ++ optionsHelp ++ commandHelp

    parts.mkString("\n\n")
  }

  def optionList(opts: Opts[_]): List[(Opt[_], Boolean)] = opts match {
    case Opts.Pure(_) => Nil
    case Opts.App(f, a) => optionList(f) ++ optionList(a)
    case Opts.OrElse(a, b) => optionList(a) ++ optionList(b)
    case Opts.Single(opt) => List(opt -> false)
    case Opts.Repeated(opt) => List(opt -> true)
    case Opts.Validate(a, _) => optionList(a)
    case Opts.Subcommand(_) => Nil
  }

  def commandList(opts: Opts[_]): List[Command[_]] = opts match {
    case Opts.Subcommand(command) => List(command)
    case Opts.App(f, a) => commandList(f) ++ commandList(a)
    case Opts.OrElse(f, a) => commandList(f) ++ commandList(a)
    case Opts.Validate(a, _) => commandList(a)
    case _ => Nil
  }

  def usage(opts: Opts[_]): List[String] =
    optionList(opts)
      .flatMap {
        case (Opt.Regular(names, metavar, _), false) => s"[${names.head} <$metavar>]" :: Nil
        case (Opt.Flag(names, _), false) => s"[${names.head}]" :: Nil
        case (Opt.Regular(names, metavar, _), true) => s"[${names.head} <$metavar>]..." :: Nil
        case (Opt.Flag(names, _), true) => s"[${names.head}]..." :: Nil
        case _ => Nil
      }

  def args(opts: Opts[_]): List[String] =
    optionList(opts)
      .flatMap {
        case (Opt.Argument(metavar), false) => s"<$metavar>" :: Nil
        case (Opt.Argument(metavar), true) => s"<$metavar>..." :: Nil
        case _ => Nil
      }

  def detail(opts: Opts[_]): List[String] =
    optionList(opts)
      .flatMap {
        case (Opt.Regular(names, metavar, help), _) => List(
          s"    ${ names.map { name => s"$name <$metavar>"}.mkString(", ") }",
          s"            $help"
        )
        case (Opt.Flag(names, help), _) => List(
          s"    ${ names.mkString(", ") }",
          s"            $help"
        )
        case _ => Nil
      }
}
