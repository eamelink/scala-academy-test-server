package controllers

import java.util.concurrent.TimeUnit

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Random

import org.joda.time.DateTime

import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Promise
import play.api.libs.iteratee.{ Concurrent, Iteratee }
import play.api.libs.json.{ JsValue, Json }
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.{ Action, Controller, WebSocket }

object Application extends Controller {

  /**
   * Action that fails fast.
   */
  def failFast = Action {
    InternalServerError("BOOM, FAST!")
  }
  
  /**
   * Action that fails after 3 seconds.
   */
  def failSlow = Action.async {
    Promise.timeout(InternalServerError("BOOM, SLOW!"), 3.seconds)
  }
  
  
  /**
   * Action that returns the exchange rate as a string.
   * 
   * Quick first 30 seconds of the minute.
   * Slow between 30 and 45 seconds.
   * Broken between 45 and 0 seconds.
   */
  def exchange1 = Action.async {
    val result = Ok(exchange1rate.toString)

    if (seconds < 30) {
      Future.successful(result)
    } else if (seconds < 45) {
      Promise.timeout(result, 3.seconds)
    } else {
      Future.failed(sys.error("BOOM!"))
    }
  }

  def exchange2Json = Json.obj(
    "timestamp" -> DateTime.now.getMillis(),
    "rate" -> exchange2rate)

  /**
   * Action that returns the exchange rate as JSON
   * 
   * Broken between 0 and 15 seconds
   * Fast between 15 and 45
   * Slow between 45 and 60
   */
  def exchange2 = Action.async {
    def result = Ok(exchange2Json)
    if (seconds < 15) {
      Future.failed(sys.error("BOOM!"))
    } else if (seconds < 45) {
      Future.successful(result)
    } else {
      Promise.timeout(result, 3.seconds)
    }

  }

  /**
   * Action that returns the exchange rate as JSON (but in a different format than `exchange2`)
   * 
   * Fast first 30 seconds of the minute
   * Slow second 30 seconds of the minute
   */
  def exchange3 = Action.async {
    val result = Ok(Json.obj(
      "bitcoin_rate" -> exchange3rate,
      "source" -> "Exchange #3"))

    if ((seconds / 10) % 2 == 0) {
      Promise.timeout(result, 50.milliseconds)
    } else {
      Promise.timeout(result, 3.seconds)
    }

  }


  /**
   * Enumerator and Channel for exchangerate live feed.
   */
  val (streamEnumerator, streamChannel) = Concurrent.broadcast[JsValue]

  /**
   * Schedule sending chunks in the feed.
   */
  Akka.system(play.api.Play.current).scheduler.schedule(0.seconds, 200.milliseconds) {
    streamChannel.push(exchange2Json)
  }
  
  def chunked = Action {
    Ok.chunked(streamEnumerator)
  }

  /**
   * Exchange rate live feed.
   */
  def stream = WebSocket.using[JsValue] { request =>
    val ignoringIteratee = Iteratee.ignore[JsValue]
    val droppingEnumeratee = Concurrent.dropInputIfNotReady[JsValue](100, TimeUnit.MILLISECONDS)
    (ignoringIteratee, streamEnumerator &> droppingEnumeratee)
  }
  
  
  def exchange1rate = 230 + Random.nextInt(40)
  def exchange2rate = 240.toDouble + DateTime.now.getSecondOfMinute().toDouble / 3 + DateTime.now.getMillisOfSecond().toDouble / 1000
  def exchange3rate = 260

  def seconds = DateTime.now.getSecondOfMinute()
  
  

}