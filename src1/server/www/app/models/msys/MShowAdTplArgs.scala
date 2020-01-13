package models.msys

import io.suggest.n2.edge.MEdge
import io.suggest.n2.node.MNode
import models.im.MImgT

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.11.15 15:51
  * Description: Модель контейнера аргуметов для шаблона [[views.html.sys1.market.ad.showAdTpl]].
  */
trait IShowAdTplArgs {

  /** Текущая рекламная карточка. */
  def mad: MNode

  /** Продьюсер рекламной карточки, если есть. */
  def producerOpt: Option[MNode]

  /** Картинки, связанные с рекламной карточкой. */
  def imgs: Seq[MImgEdge]

  /** id продьюсера карточки по данным из самой карточки. */
  def madProducerIdOpt: Option[String]

  /** Текущая карта ресиверов карточки. */
  def rcvrsCount: Int

}


/** Дефолтовая реализация модели [[IShowAdTplArgs]]. */
case class MShowAdTplArgs(
  override val mad                : MNode,
  override val producerOpt        : Option[MNode],
  override val imgs               : Seq[MImgEdge],
  override val madProducerIdOpt   : Option[String],
  override val rcvrsCount         : Int
)
  extends IShowAdTplArgs


/** Контейнер для инфы по edge'у картинки карточки. */
case class MImgEdge(edge: MEdge, img: MImgT)
