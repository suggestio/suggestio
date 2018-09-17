package io.suggest.ads

import io.suggest.adv.decl.MAdvDeclKv
import io.suggest.common.empty.EmptyUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.03.18 14:49
  * Description: Модель данных формы размещения карточки на своих узлах.
  */
object MLkAdsOneAdAdvForm {

  implicit def mLkAdsOneAdAdvFormFormat: OFormat[MLkAdsOneAdAdvForm] = {
    (__ \ "r").formatNullable[Seq[MAdvDeclKv]]
      .inmap[Seq[MAdvDeclKv]](
        EmptyUtil.opt2ImplEmpty1F( Nil ),
        { rcvrs => if (rcvrs.isEmpty) None else Some(rcvrs) }
      )
      .inmap[MLkAdsOneAdAdvForm](apply, _.decls)
  }


  implicit def univEq: UnivEq[MLkAdsOneAdAdvForm] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}


/** Контейнер данных размещения карточки.
  *
  * @param decls Список требований по размещениям в целях.
  */
case class MLkAdsOneAdAdvForm(
                               decls      : Seq[MAdvDeclKv]
                             ) {

  lazy val declsMap = decls.declsToMap

}
