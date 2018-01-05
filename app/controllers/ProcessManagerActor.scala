package controllers

import javax.inject.Inject

import akka.actor._
import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._
import sys.process._

import scala.io.Source
import scala.sys.process.Process
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.json._

object ProcessManagerActor {
  case class Runit(msg: String)
}

class ProcessManagerActor @Inject()(implicit mat: Materializer, ec: ExecutionContext)
  extends Actor with ActorLogging {

  import ProcessManagerActor._

  def receive = {
    case Runit(msg) =>
      log.info(s"ProcessManagerActor receive $msg")
      sender() ! websocketFlow
  }

  def mySource= {
    val testCommand = "sh sleep 5; echo 'hello'"
    //https://groups.google.com/forum/#!topic/akka-user/ue_GZZsed1k
    val is: java.io.OutputStream = StreamConverters.asOutputStream()
      .to(Sink.foreach(testCommand.lineStream_!)) // your logic pipeline here
      .run()
    System.setOut(new java.io.PrintStream(is))
  }

 /**
    * Generates a flow that can be used by the websocket.
    *
    * @return the flow of JSON
    */
  private lazy val websocketFlow: Flow[JsValue, JsValue, NotUsed] = {
    Flow.fromSinkAndSource(Sink.ignore, mySource).watchTermination() { (_, termination) =>
      // When the flow shuts down, make sure this actor also stops.
      termination.foreach(_ => context.stop(self))
      NotUsed
    }
  }

}