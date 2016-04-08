package models.adv.geo.cur

import models.mdt.IDateStartEnd

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.04.16 18:50
  * Description: Модель аргументов для рендера шаблона попапа.
  */
trait IGsPopupTplArgs {

  /** Список данных для рядов в попапе. */
  def rows: Seq[MPopupRowInfo]

}

case class MGsPopupTplArgs(
  override val rows: Seq[MPopupRowInfo]
)
  extends IGsPopupTplArgs


/**
  * Модель данных по одному набору размещений в рамках одного периода.
  *
  * @param intervalOpt Интервал дат размещения, если есть.
  * @param tags Гео-теги размещения, если есть.
  * @param onMainScreen Галочка "на главном экране" была установлена?
  */
case class MPopupRowInfo(
  intervalOpt     : Option[IDateStartEnd],   // По идее тут всегда Some, но вдруг...
  tags            : Seq[MTagInfo],
  onMainScreen    : Option[MOnMainScrInfo]
)

case class MTagInfo(tag: String, isOnlineNow: Boolean)
case class MOnMainScrInfo(isOnlineNow: Boolean)

