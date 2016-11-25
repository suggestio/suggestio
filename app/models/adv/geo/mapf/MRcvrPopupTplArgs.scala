package models.adv.geo.mapf

import io.suggest.mbill2.m.item.MItem
import io.suggest.model.n2.node.MNode
import models.{MNodeType, MPredicate}
import play.api.data.Form

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.11.16 17:26
  * Description: Аргументы для рендера шаблона попапа по одному узлу на карте георазмещения карточек.
  */
trait IRcvrPopupTplArgs {

  /** Целевой узел для размещения. */
  def mnode: MNode

  /** Маппинг формы с полями доступного размещения. */
  def form: Form[MRcvrPopupFormRes]

  /** Инфа о текущих оплаченных размещениях на узле. */
  def currAdvs: Seq[MItem]

}


/** Модель аргументов рендера шаблона popup'а для узла карты георазмещения. */
case class MRcvrPopupTplArgs(
  override val mnode    : MNode,
  override val form     : Form[MRcvrPopupFormRes],
  override val currAdvs : Seq[MItem]
)
  extends IRcvrPopupTplArgs


/**
  * Модель результата биндинга формы.
  * @param subNodes Инфа о размещениях.
  */
case class MRcvrPopupFormRes(
  subNodes      : List[MSubNodeFormInfo]
)

/** Инфа по размещениям, доступным для выбора юзером. */
case class MSubNodeFormInfo(
  ntype     : MNodeType,
  advType   : MPredicate,
  nodeId    : String,
  selected  : Boolean
)
