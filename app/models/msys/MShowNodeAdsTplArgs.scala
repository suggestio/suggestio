package models.msys

import io.suggest.mbill2.m.item.MItem
import models.{blk, AdSearch, MNode}

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
  def a: AdSearch

  /** Карта данных по размещениям карточек. */
  def ad2advMap: Map[String, Traversable[MItem]]

}


/** Дефолтовая реализация модели [[IShowNodeAdsTplArgs]]. */
case class MShowNodeAdsTplArgs(
  override val mads         : Seq[blk.IRenderArgs],
  override val nodeOpt      : Option[MNode],
  override val rcvrsMap     : Map[String, Seq[MNode]],
  override val a            : AdSearch,
  override val ad2advMap    : Map[String, Traversable[MItem]]
)
  extends IShowNodeAdsTplArgs
