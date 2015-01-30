package io.suggest.proto.bixo

import akka.actor.Actor
import io.suggest.util.JacksonWrapper
import io.suggest.model.JsonMap_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.11.13 10:53
 * Description: Для отображения данных по дереву акторов в веб-морде используется этот простой интерфейс и протокол.
 * Основная идея: чтобы в веб-морде можно было гулять по дереву акторов, читать ихнее состояние.
 */
trait TalkativeActor extends Actor {

  def receive: Actor.Receive = {
    case TA_GetDirectChildren => handleTAGetDirectChildren()
    case TA_GetStatusReport   => handleTAGetStatusReport()
  }
  
  def handleTAGetDirectChildren() {
    sender ! taGetDirectChildren
  }
  def taGetDirectChildren: List[String] = context.children.map(_.path.name).toList


  def handleTAGetStatusReport() {
    val reply = JacksonWrapper.serializePretty(taGetStatusReport)
    sender ! reply
  }

  def taGetStatusReport: JsonMap_t
}


case object TA_GetDirectChildren extends Serializable
case object TA_GetStatusReport extends Serializable
