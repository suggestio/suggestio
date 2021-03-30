package io.suggest.sc.m.grid

import diode.data.Pot
import io.suggest.scalaz.NodePath_t
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens
import scalaz.{Tree, TreeLoc}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.03.2021 10:04
  * Description: Модели для поддержания карточек в выдаче.
  */
object MGridAds {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MGridAds] = UnivEq.derive

  def idCounter = GenLens[MGridAds](_.idCounter)
  def adsTreePot = GenLens[MGridAds](_.adsTreePot)
  def interactWith = GenLens[MGridAds](_.interactWith)

}


/** Состояние выдачи карточек.
  * Разделение карточек на карту и список ключей нужен для возможности безопасного рендера одной и той же карточки
  * в одной выдаче нескольку раз.
  *
  * @param idCounter Счётчик ключей карточек. Ключи в adsMap получаются из этого счётчика.
  *                  Здесь хранится следующее значение, которое проинкрементировалось, но ещё не использовалось.
  * @param adsTreePot Дерево данных для плитки, описывает текущую плитку.
  *                  Каждый элемент дерева имеет список поддеревьев в MScAdData.items.MJdDoc.
  *                  Рендер плитки идёт по самому нижнему уровню указателей, и каждый уровень как-либо
  *                  переписывает представление верхнего уровня.
  *                  Например, вместо одного блока (одной карточки) можно добавить child-элементы, и отрендерить целую карточку,
  *                  или какие-то указанные блоки, или любое другое поддерево по нижнему ярусу этого под-дерева.
  *                  При сворачивании уровня - исходный элемент верхнего уровня (карточка, main-блок и т.д.) снова становятся на место.
  *
  *                  В таком дереве возможны необычные операции: Разборка указателя на всю карточку на список из блоков карточки.
  *                  И любой из этих блоков можно перезаписать другой под-карточкой. И т.д.
  * @param interactWith Путь в дереве до карточки и ключ элемента плитки в карточке, с которым происходит
  *                     какое-либо текущее взаимодействие юзера.
  *                     Это можно использовать, как источник информации о текущей фокусировке.
  *                     И сюда же обычно проскролливается плитка.
  */
final case class MGridAds(
                           idCounter            : GridAdKey_t                         = 0,
                           adsTreePot           : Pot[Tree[MScAdData]]                = Pot.empty,
                           interactWith         : Option[(NodePath_t, GridAdKey_t)]   = None,
                         ) {

  /** Карточка, с которой происходит взаимодействие, если есть. */
  lazy val interactAdOpt: Option[TreeLoc[MScAdData]] = {
    for {
      (gridPath, _) <- interactWith
      adsTree <- adsTreePot.toOption
      scAdLoc <- adsTree.loc.findByGridKeyPath( gridPath.iterator )
    } yield {
      scAdLoc
    }
  }

  /** Происходит ли сейчас загрузка какой-либо карточки? */
  lazy val _adsHasPending: Boolean = {
    adsTreePot
      .iterator
      .exists(_.rootLabel.data.isPending)
  }

  /** Происходит ли сейчас загрузка какой-либо карточки или карточек? */
  def adsHasPending: Boolean =
    adsTreePot.isPending || _adsHasPending

}
