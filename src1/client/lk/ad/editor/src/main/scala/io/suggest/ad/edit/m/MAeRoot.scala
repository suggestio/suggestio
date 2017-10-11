package io.suggest.ad.edit.m

import diode.FastEq
import io.suggest.ad.edit.m.pop.MAePopupsS
import io.suggest.spa.delay.MDelayerS
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ws.pool.m.MWsPoolS
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:30
  * Description: Корневая модель состояния формы редактора рекламной карточки.
  */
object MAeRoot {

  implicit object MAeRootFastEq extends FastEq[MAeRoot] {
    override def eqv(a: MAeRoot, b: MAeRoot): Boolean = {
      (a.conf ===* b.conf) &&
        (a.doc ===* b.doc) &&
        (a.popups ===* b.popups) &&
        (a.wsPool ===* b.wsPool)
    }
  }

  implicit def univEq: UnivEq[MAeRoot] = UnivEq.derive

}


/** Класс корневой модели состояния редактора рекламной карточки.
  *
  * @param conf Конфиг, присланный сервером.
  * @param doc Состояние редактирования документа. Там почти всё и живёт.
  * @param popups Состояние попапов.
  * @param wsPool Пул коннекшенов.
  */
case class MAeRoot(
                    conf        : MAdEditFormConf,
                    doc         : MDocS,
                    popups      : MAePopupsS        = MAePopupsS.empty,
                    wsPool      : MWsPoolS          = MWsPoolS.empty,
                    //delayer   : MDelayerS         = MDelayerS.default
                  ) {

  /** Экспорт данных формы. */
  def toForm: MAdEditForm = ???

  def withDoc(doc: MDocS)                     = copy(doc = doc)
  def withPopups(popups: MAePopupsS)          = copy(popups = popups)
  def withWsPool(wsPool: MWsPoolS)            = copy(wsPool = wsPool)
  //def withDelayer(delayer: MDelayerS)         = copy(delayer = delayer)

}
