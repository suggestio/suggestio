package io.suggest.xadv.ext.js.runner.m.ex

import io.suggest.xadv.ext.js.runner.m.MErrorInfoT

import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.02.15 10:45
 * Description: Ошибка при взаимодействии с login API удалённого сервиса. Это именно ошибка API, а не что-то иное.
 */
case class LoginApiException(
  override val getMessage: String,
  override val getCause: Throwable
) extends Exception with MErrorInfoT {

  override def msg: String = "e.adv.ext.api.login"

  override def args = Seq.empty

  override def info = Some(Dictionary[Any](
    "msg"     -> getMessage,
    "eclass"  -> getCause.getClass.getName,
    "emsg"    -> getCause.getMessage
  ))

}
