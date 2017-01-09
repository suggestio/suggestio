package io.suggest.sc

import io.suggest.common.menum.{EnumMaybeWithName, ILightEnumeration, LightEnumeration, StrIdValT}
import io.suggest.sc.ScConstants.Resp._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.09.16 11:28
  * Description: Общий клиент-серверный код модели типов sc-resp-экшенов.
  */
trait MScRespActionTypesT extends ILightEnumeration with StrIdValT {

  override type T <: ValT

  /** Тип экшена с индексом выдачи. */
  val Index   : T = _instance( INDEX_RESP_ACTION )

  /** Тип экшена с плиткой выдачи. */
  val AdsTile   : T = _instance( ADS_TILE_RESP_ACTION )

  /** Тип экшена с карточками focused-выдачи. */
  val AdsFoc    : T = _instance( FOC_ANSWER_ACTION )

  protected[this] def _instance(strId: String): T

}


/** Трейт модели для server-side. */
trait MScRespActionTypesEnumT extends EnumMaybeWithName with MScRespActionTypesT {

  protected[this] class Val(override val strId: String)
    extends super.Val(strId)
    with ValT

  override type T = Val

  override protected[this] def _instance(strId: String): Val = {
    new Val(strId)
  }

}


/** Облегчённый трейт модели для нужд js. */
trait MScRespActionTypesLightT extends LightEnumeration with MScRespActionTypesT {

  protected[this] class Val(override val strId: String)
    extends super.ValT

  override type T = Val

  override protected[this] def _instance(strId: String): Val = {
    new Val(strId)
  }

  override def maybeWithName(n: String): Option[Val] = {
    n match {
      case Index.strId    => Some(Index)
      case AdsTile.strId  => Some(AdsTile)
      case AdsFoc.strId   => Some(AdsFoc)
      case _              => None
    }
  }

}
