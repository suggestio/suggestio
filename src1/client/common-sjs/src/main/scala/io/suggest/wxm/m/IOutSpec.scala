package io.suggest.wxm.m

import boopickle.Pickler
import io.suggest.wxm.MWxmMsg
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.07.17 16:04
  * Description: Система поддержки заворачивания данных и wxm-msg.
  */
trait IOutSpec[T] {

  def payload: T


  def booPickler: Pickler[T]

}

trait IOutSpecJson[T] extends IOutSpec[T] {

  implicit def jsonPickler: Format[T]

  def jsonWxmMsg: MWxmMsg[T] = {
  }

}

