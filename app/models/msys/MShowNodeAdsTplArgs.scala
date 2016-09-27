package models.msys

import io.suggest.mbill2.m.item.MItem
import io.suggest.model.n2.node.MNode
import io.suggest.model.n2.node.search.MNodeSearch
import models.msc.MScAdsSearchQs
import models.blk

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.11.15 12:28
  * Description: Модель контейнера аргументов для шаблона [[views.html.sys1.market.adn.showAdnNodeAdsTpl]].
  */
trait IShowNodeAdsTplArgs {

  /** Карточки для рендера. */
  def mads: Seq[blk.IRenderArgs]

  /** Узел, если просмотр в рамках одного узла. */
  def nodeOpt: Option[MNode]

  /** Карта с ресиверами по id. */
  def rcvrsMap: Map[String, Seq[MNode]]

  /** Данные запрошенного поиска. */
  def qs: MScAdsSearchQs

  /** Карта данных по размещениям карточек. */
  def ad2advMap: Map[String, Traversable[MItem]]

  /** Скомпиленные данные поиска. */
  def msearch: MNodeSearch

}


/** Дефолтовая реализация модели [[IShowNodeAdsTplArgs]]. */
case class MShowNodeAdsTplArgs(
                                override val mads         : Seq[blk.IRenderArgs],
                                override val nodeOpt      : Option[MNode],
                                override val rcvrsMap     : Map[String, Seq[MNode]],
                                override val qs           : MScAdsSearchQs,
                                override val ad2advMap    : Map[String, Traversable[MItem]],
                                override val msearch      : MNodeSearch
)
  extends IShowNodeAdsTplArgs
