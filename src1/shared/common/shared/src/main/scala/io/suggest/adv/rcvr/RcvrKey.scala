package io.suggest.adv.rcvr

import io.suggest.adv.geo.RcvrsMap_t
import io.suggest.common.html.HtmlConstants
import io.suggest.es.model.MEsUuId
import io.suggest.scalaz.ScalazUtil
import japgolly.univeq.UnivEq
import scalaz.{Monoid, Validation, ValidationNel}
import scalaz.syntax.apply._
import scalaz.std.iterable._
import scalaz.std.string._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 22:34
  * Description: Статическая утиль для RcvrKey-модели.
  */
object RcvrKey {

  def rcvrKey2urlPath(rcvrKey: RcvrKey): String = {
    rcvrKey.mkString( HtmlConstants.SLASH )
  }

  def from(rcvrKeySeq: Seq[String]) =
    rcvrKeySeq.toList

  // Затычка для scalaz, чтобы можно было провалидировать коллекцию из RcvrKey.
  implicit object rcvrKeyDirtyMonoid extends Monoid[RcvrKey] {
    override def zero: RcvrKey = Nil
    override def append(f1: RcvrKey, f2: => RcvrKey): RcvrKey = f2
  }

  def rcvrsMapV(rm: RcvrsMap_t, maxMapSz: Int): ValidationNel[String, RcvrsMap_t] = {
    var vld = Validation.liftNel(rm)( _.size > maxMapSz, "e.rcvrs.too.many" )
    // Провалидировать все ключи, если они есть.
    if (rm.keys.nonEmpty) {
      // Затычка для scalaz, чтобы можно было провалидировать коллекцию из RcvrKey.
      val v2 = ScalazUtil.validateAll(rm.keys)(rcvrKeyV)
      vld = (vld |@| v2) { (_,_) => rm }
    }
    vld
  }

  def rcvrIdV(rcvrId: String): ValidationNel[String, String] = {
    Validation.liftNel(rcvrId)( !MEsUuId.isEsIdValid(_), "e.rcvr.id.format" )
  }

  def rcvrKeyV(rcvrKey: RcvrKey): ValidationNel[String, RcvrKey] = {
    val v1 = Validation.liftNel(rcvrKey)(_.isEmpty, "e.rcvr.key.empty")
    val v2 = ScalazUtil.validateAll(rcvrKey)(rcvrIdV)
    (v1 |@| v2) { (_,_) => rcvrKey }
  }

  @inline implicit def univEq: UnivEq[RcvrKey] = UnivEq.derive

}
