package io.suggest.lk.adv.m

import diode.FastEq
import diode.data.Pot
import io.suggest.bill.MGetPriceResp
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.01.17 11:27
  * Description: Модель состояния цены и состояния её рассчёта.
  */
object MPriceS {

  implicit object MPriceSFastEq extends FastEq[MPriceS] {
    override def eqv(a: MPriceS, b: MPriceS): Boolean = {
      a.resp ===* b.resp
    }
  }

  @inline implicit def univEq: UnivEq[MPriceS] = UnivEq.derive

}


/**
  * Класс модели состояния компонента цены.
  * @param resp Контейнер ответа сервера по запрос рассчёта стоимости [размещения].
  * @param reqTsId id реквеста, ожидаемого в текущий момент.
  *                Используется вместо pot.Pending.startTime из-за ожидания [[https://github.com/ochrons/diode/pull/38]].
  */
case class MPriceS(
                    reqTsId  : Option[Long]        = None,
                    resp     : Pot[MGetPriceResp]  = Pot.empty
                  )
{

  def withPriceResp(pr2: Pot[MGetPriceResp]) = copy(resp = pr2)

  def respDslOpt = resp.toOption.flatMap(_.priceDsl)

}