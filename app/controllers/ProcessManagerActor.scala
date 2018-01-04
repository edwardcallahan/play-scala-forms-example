package controllers

import akka.actor._
import scala.io.Source
import scala.concurrent.Future
import scala.sys.process.Process
import scala.concurrent.ExecutionContext.Implicits.global
import sys.process._
import play.api.libs.concurrent.InjectedActorSupport

object ProcessManagerActor {
  def props(out: ActorRef) = Props(new ProcessManagerActor(out))

  case class Runit(msg: String)
}


class ProcessManagerActor (sender: ActorRef) extends Actor with InjectedActorSupport {

  import ProcessManagerActor._

  def receive = {
    case Runit(msg) =>
      sender ! test
  }

  def test:String = {
    //https://stackoverflow.com/questions/41534009/how-to-write-a-string-to-scala-process
    val calcCommand = "bc"
    // strings are implicitly converted to ProcessBuilder
    // via scala.sys.process.ProcessImplicits.stringToProcess(_)
    val calcProc = calcCommand.run(new ProcessIO(
      // Handle subprocess's stdin
      // (which we write via an OutputStream)
      in => {
        val writer = new java.io.PrintWriter(in)
        writer.println("1 + 2")
        writer.println("3 + 4")
        writer.close()
      },
      // Handle subprocess's stdout
      // (which we read via an InputStream)
      out => {
        val src = scala.io.Source.fromInputStream(out)
        for (line <- src.getLines()) {
          println("Answer: " + line)
        }
        src.close()
      },
      // We don't want to use stderr, so just close it.
      _.close()
    ))

    // Using ProcessBuilder.run() will automatically launch
    // a new thread for the input/output routines passed to ProcessIO.
    // We just need to wait for it to finish.

    val code = calcProc.exitValue()

    (s"Subprocess exited with code $code.")
  }

  def out = (output: java.io.OutputStream) => {
    output.flush()
    output.close()
  }

  def in = (input: java.io.InputStream) => {
    println("Stdout: " + Source.fromInputStream(input).mkString)
    input.close()
  }

}