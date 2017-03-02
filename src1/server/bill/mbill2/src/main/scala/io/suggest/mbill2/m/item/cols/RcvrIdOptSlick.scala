package io.suggest.mbill2.m.item.cols

import io.suggest.slick.profile.IProfile

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.12.15 22:20
  * Description: Поддержка поля таблицы rcvrIdOpt: Option[rcvr_id].
  */
trait RcvrIdOptSlick extends IProfile {

  import profile.api._

  def RCVR_ID_FN = "rcvr_id"

  trait RcvrIdOptColumn { that: Table[_] =>
    def rcvrIdOpt = column[Option[String]](RCVR_ID_FN)
  }

}

trait IRcvrIdOpt {
  def rcvrIdOpt: Option[String]
}
