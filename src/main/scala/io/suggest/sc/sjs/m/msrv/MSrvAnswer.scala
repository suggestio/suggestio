package io.suggest.sc.sjs.m.msrv

import scala.scalajs.js._
import io.suggest.sc.ScConstants.Resp.ACTION_FN

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.08.15 12:34
 * Description: Доступ к ответам сервера через общие поля.
 */

trait SrvAnswerT {

  def json: WrappedDictionary[Any]

  def actionOpt: Option[String] = {
    json.get(ACTION_FN)
      .map(_.toString)
  }

}


case class MSrvAnswer(
  override val json: WrappedDictionary[Any]
)
  extends SrvAnswerT
