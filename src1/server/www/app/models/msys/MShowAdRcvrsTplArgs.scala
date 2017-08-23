package models.msys

import io.suggest.model.n2.edge.MEdge
import io.suggest.model.n2.node.MNode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.11.15 14:59
 * Description: Модель контейнера аргументов для шаблона [[views.html.sys1.market.ad.showAdRcvrsTpl]].
 */
trait IShowAdRcvrsTplArgs {

  /** Текущая рекламная карточка. */
  def mad         : MNode

  /** Рассчетная карта ресиверов. */
  def newRcvrsMap : Seq[MEdge]

  /** Карта узлов. */
  def nodesMap    : Map[String, MNode]

  /** Продьюсер карточки (наверное), если есть. */
  def nodeOpt     : Option[MNode]

  /** Карта ресиверов. */
  def rcvrsMap    : Seq[MEdge]

  /** В норме ли текущая карта ресиверов? */
  def rcvrsMapOk  : Boolean

}


/** Дефолтовая реализация модели [[IShowAdRcvrsTplArgs]]. */
case class MShowAdRcvrsTplArgs(
  override val mad         : MNode,
  override val newRcvrsMap : Seq[MEdge],
  override val nodesMap    : Map[String, MNode],
  override val nodeOpt     : Option[MNode],
  override val rcvrsMap    : Seq[MEdge],
  override val rcvrsMapOk  : Boolean
)
  extends IShowAdRcvrsTplArgs
