package io.suggest.xadv.ext.js.runner.m

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 12:20
 * Description: Модель, описывающая входящую команду от ws-сервера.
 */
case class MJsCommand(`type`: String,
                      data: String,
                      replyTo: Option[String] = None)

object MJsCommand {

  def fromString(s: String): MJsCommand = {
    upickle.read[MJsCommand](s)
  }

}
